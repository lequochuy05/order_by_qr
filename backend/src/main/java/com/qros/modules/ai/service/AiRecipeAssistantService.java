package com.qros.modules.ai.service;

import static com.qros.modules.ai.support.AiJsonExtractor.isEnglish;
import static com.qros.modules.ai.support.AiJsonExtractor.normalizeLanguage;
import static com.qros.modules.ai.support.AiJsonExtractor.readStringList;
import static com.qros.modules.ai.support.AiJsonExtractor.readText;
import static com.qros.modules.ai.support.AiJsonExtractor.truncate;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.request.AiRecipeAssistantRequest;
import com.qros.modules.ai.dto.request.AiRecipeAssistantRequest.DraftRecipeLine;
import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse;
import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse.FoodCostAnalysis;
import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse.SuggestedRecipeLine;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.ai.support.NoteSafetyAnalyzer;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AiRecipeAssistantService {

    private static final BigDecimal TARGET_FOOD_COST_RATIO = new BigDecimal("0.35");
    private static final BigDecimal ROUNDING_UNIT = new BigDecimal("1000");
    private static final double WARNING_THRESHOLD_PERCENT = 35.0;
    private static final double CRITICAL_THRESHOLD_PERCENT = 45.0;
    private static final int MAX_SUGGESTED_ITEMS = 8;
    private static final int MAX_INVENTORY_CONTEXT = 60;

    private final MenuItemRepository menuItemRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiRecipeAssistantService(
            MenuItemRepository menuItemRepository,
            RecipeItemRepository recipeItemRepository,
            InventoryItemRepository inventoryItemRepository,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.menuItemRepository = menuItemRepository;
        this.recipeItemRepository = recipeItemRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "recipe-assistant")
                .description("AI recipe BOM and food cost assistant requests")
                .register(meterRegistry);
    }

    public AiRecipeAssistantResponse assistRecipe(AiRecipeAssistantRequest request) {
        if (request == null || request.menuItemId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Menu item id is required");
        }
        String language = normalizeLanguage(request.language());
        LocalDateTime now = AppTime.now();

        MenuItem menuItem = menuItemRepository
                .findById(request.menuItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));

        BigDecimal sellingPrice = safeScale(menuItem.getPrice(), 2);
        List<InventoryItem> activeInventory = inventoryItemRepository.findByActiveTrueOrderByNameAsc();
        Map<Long, InventoryItem> activeInventoryById = activeInventory.stream()
                .collect(Collectors.toMap(
                        InventoryItem::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<SuggestedRecipeLine> currentLines = resolveCurrentLines(request, activeInventoryById);
        FoodCostAnalysis currentAnalysis = computeFoodCostAnalysis(sellingPrice, currentLines);

        String context = buildRecipeContext(menuItem, sellingPrice, currentLines, currentAnalysis, activeInventory);
        String systemPrompt = promptBuilder.buildRecipeBomPrompt(context, language);
        String rawReply = aiChatGateway.chat(
                new AiChatRequest("Analyze food cost and suggest BOM for %s.".formatted(menuItem.getName()), List.of()),
                systemPrompt,
                "");
        aiRequestsCounter.increment();

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildDeterministicFallback(
                    menuItem, sellingPrice, language, currentLines, currentAnalysis, activeInventory, now);
        }

        AiRecipeAssistantResponse parsed = tryParseAiResponse(
                menuItem,
                sellingPrice,
                language,
                currentLines,
                currentAnalysis,
                activeInventoryById,
                activeInventory,
                rawReply,
                now);
        return parsed != null
                ? parsed
                : buildDeterministicFallback(
                        menuItem, sellingPrice, language, currentLines, currentAnalysis, activeInventory, now);
    }

    private List<SuggestedRecipeLine> resolveCurrentLines(
            AiRecipeAssistantRequest request, Map<Long, InventoryItem> activeInventoryById) {
        if (request.currentItems() != null && !request.currentItems().isEmpty()) {
            List<Long> missingIds = request.currentItems().stream()
                    .map(DraftRecipeLine::inventoryItemId)
                    .filter(id -> id != null && !activeInventoryById.containsKey(id))
                    .distinct()
                    .toList();
            Map<Long, InventoryItem> combinedMap = new LinkedHashMap<>(activeInventoryById);
            if (!missingIds.isEmpty()) {
                inventoryItemRepository
                        .findAllById(missingIds)
                        .forEach(item -> combinedMap.putIfAbsent(item.getId(), item));
            }
            Map<Long, SuggestedRecipeLine> deduped = new LinkedHashMap<>();
            for (DraftRecipeLine line : request.currentItems()) {
                if (line.inventoryItemId() == null
                        || line.quantityRequired() == null
                        || line.quantityRequired().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                InventoryItem inv = combinedMap.get(line.inventoryItemId());
                if (inv == null) {
                    continue;
                }
                BigDecimal qty = line.quantityRequired().setScale(3, RoundingMode.HALF_UP);
                deduped.putIfAbsent(inv.getId(), toLine(inv, qty, ""));
            }
            return List.copyOf(deduped.values());
        }

        List<RecipeItem> savedRecipes = recipeItemRepository.findByMenuItemId(request.menuItemId());
        return savedRecipes.stream()
                .filter(r -> r.getInventoryItem() != null
                        && r.getQuantityRequired() != null
                        && r.getQuantityRequired().compareTo(BigDecimal.ZERO) > 0)
                .map(r -> toLine(r.getInventoryItem(), r.getQuantityRequired().setScale(3, RoundingMode.HALF_UP), ""))
                .toList();
    }

    private AiRecipeAssistantResponse tryParseAiResponse(
            MenuItem menuItem,
            BigDecimal sellingPrice,
            String language,
            List<SuggestedRecipeLine> currentLines,
            FoodCostAnalysis currentAnalysis,
            Map<Long, InventoryItem> activeInventoryById,
            List<InventoryItem> activeInventory,
            String rawReply,
            LocalDateTime generatedAt) {
        String extracted = AiJsonExtractor.extractJsonObject(rawReply);
        if (extracted == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extracted);
            List<SuggestedRecipeLine> suggested = new ArrayList<>();
            Set<Long> seenIds = new LinkedHashSet<>();
            JsonNode itemsNode = root.path("suggestedItems");
            if (itemsNode.isArray()) {
                for (JsonNode entry : itemsNode) {
                    long invId = entry.path("inventoryItemId").asLong(-1L);
                    InventoryItem inv = activeInventoryById.get(invId);
                    if (inv == null || !seenIds.add(invId)) {
                        continue;
                    }
                    BigDecimal qty = readPositiveDecimal(entry.path("quantityRequired"));
                    if (qty == null) {
                        continue;
                    }
                    String reason = readText(entry.path("reason"), "");
                    suggested.add(toLine(inv, qty, truncate(reason, 120)));
                    if (suggested.size() >= MAX_SUGGESTED_ITEMS) {
                        break;
                    }
                }
            }

            List<String> costWarnings = readStringList(root.path("costWarnings"), 4);
            List<String> pricingAdvice = readStringList(root.path("pricingAdvice"), 4);

            if (suggested.isEmpty()) {
                // Keep valid current/keyword lines rather than accepting an invalid hallucinated BOM
                List<SuggestedRecipeLine> safeFallbackLines = !currentLines.isEmpty()
                        ? currentLines
                        : matchIngredientsByName(menuItem.getName(), activeInventory, language);
                if (safeFallbackLines.isEmpty() && costWarnings.isEmpty() && pricingAdvice.isEmpty()) {
                    return null;
                }
                FoodCostAnalysis safeAnalysis = computeFoodCostAnalysis(sellingPrice, safeFallbackLines);
                return new AiRecipeAssistantResponse(
                        menuItem.getId(),
                        menuItem.getName(),
                        sellingPrice,
                        language,
                        currentAnalysis,
                        safeAnalysis,
                        safeFallbackLines,
                        costWarnings.isEmpty()
                                ? buildDefaultCostWarnings(sellingPrice, currentAnalysis, safeAnalysis, language)
                                : costWarnings,
                        pricingAdvice.isEmpty()
                                ? buildDefaultPricingAdvice(sellingPrice, currentAnalysis, safeAnalysis, language)
                                : pricingAdvice,
                        generatedAt,
                        false);
            }

            FoodCostAnalysis suggestedAnalysis = computeFoodCostAnalysis(sellingPrice, suggested);
            return new AiRecipeAssistantResponse(
                    menuItem.getId(),
                    menuItem.getName(),
                    sellingPrice,
                    language,
                    currentAnalysis,
                    suggestedAnalysis,
                    suggested,
                    costWarnings.isEmpty()
                            ? buildDefaultCostWarnings(sellingPrice, currentAnalysis, suggestedAnalysis, language)
                            : costWarnings,
                    pricingAdvice.isEmpty()
                            ? buildDefaultPricingAdvice(sellingPrice, currentAnalysis, suggestedAnalysis, language)
                            : pricingAdvice,
                    generatedAt,
                    true);
        } catch (Exception ex) {
            log.debug("Failed to parse AI recipe assistant JSON payload: {}", ex.getMessage());
            return null;
        }
    }

    private AiRecipeAssistantResponse buildDeterministicFallback(
            MenuItem menuItem,
            BigDecimal sellingPrice,
            String language,
            List<SuggestedRecipeLine> currentLines,
            FoodCostAnalysis currentAnalysis,
            List<InventoryItem> activeInventory,
            LocalDateTime generatedAt) {
        List<SuggestedRecipeLine> suggestedLines = !currentLines.isEmpty()
                ? annotateExistingLines(currentLines, language)
                : matchIngredientsByName(menuItem.getName(), activeInventory, language);
        FoodCostAnalysis suggestedAnalysis = computeFoodCostAnalysis(sellingPrice, suggestedLines);

        return new AiRecipeAssistantResponse(
                menuItem.getId(),
                menuItem.getName(),
                sellingPrice,
                language,
                currentAnalysis,
                suggestedAnalysis,
                suggestedLines,
                buildDefaultCostWarnings(sellingPrice, currentAnalysis, suggestedAnalysis, language),
                buildDefaultPricingAdvice(sellingPrice, currentAnalysis, suggestedAnalysis, language),
                generatedAt,
                false);
    }

    static FoodCostAnalysis computeFoodCostAnalysis(BigDecimal sellingPrice, List<SuggestedRecipeLine> lines) {
        BigDecimal safePrice = safeScale(sellingPrice, 2);
        if (lines == null || lines.isEmpty()) {
            return new FoodCostAnalysis(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    null,
                    safePrice,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    "NO_RECIPE");
        }

        BigDecimal totalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int missingUnitCostCount = 0;
        for (SuggestedRecipeLine line : lines) {
            BigDecimal unitCost = safeScale(line.unitCost(), 2);
            if (unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                missingUnitCostCount++;
            }
            BigDecimal lineCost = unitCost.multiply(line.quantityRequired()).setScale(2, RoundingMode.HALF_UP);
            totalCost = totalCost.add(lineCost);
        }
        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossProfit = safePrice.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
        Double foodCostPercent = null;
        if (safePrice.compareTo(BigDecimal.ZERO) > 0) {
            foodCostPercent = totalCost
                    .multiply(BigDecimal.valueOf(100))
                    .divide(safePrice, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        BigDecimal recommendedMinPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rawTarget = totalCost.divide(TARGET_FOOD_COST_RATIO, 4, RoundingMode.CEILING);
            recommendedMinPrice = rawTarget
                    .divide(ROUNDING_UNIT, 0, RoundingMode.CEILING)
                    .multiply(ROUNDING_UNIT)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        String marginStatus;
        if (missingUnitCostCount > 0) {
            marginStatus = "MISSING_COST";
        } else if (foodCostPercent != null && foodCostPercent > CRITICAL_THRESHOLD_PERCENT) {
            marginStatus = "CRITICAL";
        } else if (foodCostPercent != null && foodCostPercent > WARNING_THRESHOLD_PERCENT) {
            marginStatus = "WARNING";
        } else {
            marginStatus = "HEALTHY";
        }

        return new FoodCostAnalysis(
                totalCost, foodCostPercent, grossProfit, recommendedMinPrice, missingUnitCostCount, marginStatus);
    }

    private List<SuggestedRecipeLine> annotateExistingLines(List<SuggestedRecipeLine> lines, String language) {
        String reason =
                isEnglish(language) ? "Verified from current BOM line" : "Giữ theo định mức nguyên liệu hiện tại";
        return lines.stream()
                .map(line -> new SuggestedRecipeLine(
                        line.inventoryItemId(),
                        line.inventoryItemName(),
                        line.unit(),
                        line.quantityRequired(),
                        line.unitCost(),
                        line.lineCost(),
                        line.reason() != null && !line.reason().isBlank() ? line.reason() : reason))
                .toList();
    }

    private List<SuggestedRecipeLine> matchIngredientsByName(
            String dishName, List<InventoryItem> activeInventory, String language) {
        if (dishName == null || dishName.isBlank() || activeInventory.isEmpty()) {
            return List.of();
        }
        String normalizedDish = foldForMatching(dishName);
        Set<String> dishTokens = Arrays.stream(normalizedDish.split(" "))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dishTokens.isEmpty()) {
            return List.of();
        }

        List<SuggestedRecipeLine> matches = new ArrayList<>();
        for (InventoryItem inv : activeInventory) {
            String normalizedInv = foldForMatching(inv.getName());
            boolean tokenHit = normalizedDish.contains(normalizedInv)
                    || dishTokens.stream().anyMatch(normalizedInv::contains);
            if (!tokenHit) {
                continue;
            }
            BigDecimal defaultQty = defaultQuantityForUnit(inv.getUnit());
            String reason =
                    isEnglish(language) ? "Matched ingredient keyword in dish name" : "Khớp tên nguyên liệu từ tên món";
            matches.add(toLine(inv, defaultQty, reason));
            if (matches.size() >= 4) {
                break;
            }
        }
        return matches;
    }

    private List<String> buildDefaultCostWarnings(
            BigDecimal sellingPrice, FoodCostAnalysis current, FoodCostAnalysis target, String language) {
        List<String> warnings = new ArrayList<>();
        FoodCostAnalysis focus = !"NO_RECIPE".equals(target.marginStatus()) ? target : current;
        if ("NO_RECIPE".equals(focus.marginStatus())) {
            warnings.add(
                    isEnglish(language)
                            ? "This dish has no BOM recipe yet; add ingredients to track true food cost and margin."
                            : "Món này chưa có định mức nguyên liệu (BOM); hãy thêm nguyên liệu để tính giá vốn chính xác.");
            return warnings;
        }
        if (focus.missingUnitCostCount() > 0) {
            warnings.add(
                    isEnglish(language)
                            ? "%d ingredient(s) have unit cost 0; update unit prices in Inventory to prevent distorted margins."
                                    .formatted(focus.missingUnitCostCount())
                            : "%d nguyên liệu chưa khai báo đơn giá nhập (0đ); cần cập nhật để tránh sai lệch biên lợi nhuận."
                                    .formatted(focus.missingUnitCostCount()));
        }
        if (focus.foodCostPercent() != null && focus.foodCostPercent() > CRITICAL_THRESHOLD_PERCENT) {
            warnings.add(
                    isEnglish(language)
                            ? "Food cost ratio is %.1f%% (>45%% critical threshold), squeezing gross profit."
                                    .formatted(focus.foodCostPercent())
                            : "Tỷ lệ giá vốn %.1f%% vượt ngưỡng rủi ro (>45%%), biên lợi nhuận gộp bị thu hẹp."
                                    .formatted(focus.foodCostPercent()));
        } else if (focus.foodCostPercent() != null && focus.foodCostPercent() > WARNING_THRESHOLD_PERCENT) {
            warnings.add(
                    isEnglish(language)
                            ? "Food cost ratio is %.1f%% (above 35%% target).".formatted(focus.foodCostPercent())
                            : "Tỷ lệ giá vốn %.1f%% đang cao hơn mức mục tiêu 35%%."
                                    .formatted(focus.foodCostPercent()));
        } else if (warnings.isEmpty()) {
            warnings.add(
                    isEnglish(language)
                            ? "Food cost is healthy at %.1f%% vs selling price %s."
                                    .formatted(
                                            focus.foodCostPercent() != null ? focus.foodCostPercent() : 0.0,
                                            sellingPrice.toPlainString())
                            : "Tỷ lệ giá vốn đang ở mức an toàn (%.1f%%)."
                                    .formatted(focus.foodCostPercent() != null ? focus.foodCostPercent() : 0.0));
        }
        return warnings;
    }

    private List<String> buildDefaultPricingAdvice(
            BigDecimal sellingPrice, FoodCostAnalysis current, FoodCostAnalysis target, String language) {
        List<String> advice = new ArrayList<>();
        FoodCostAnalysis focus = !"NO_RECIPE".equals(target.marginStatus()) ? target : current;
        if (focus.recommendedMinPrice().compareTo(sellingPrice) > 0
                && focus.totalFoodCost().compareTo(BigDecimal.ZERO) > 0) {
            String recPrice = "%,.0fđ".formatted(focus.recommendedMinPrice()).replace(",", ".");
            advice.add(
                    isEnglish(language)
                            ? "Consider raising menu price to at least %s to keep food cost <= 35%%."
                                    .formatted(recPrice)
                            : "Cân nhắc điều chỉnh giá bán tối thiểu lên %s để giữ giá vốn <= 35%%."
                                    .formatted(recPrice));
        } else if ("HEALTHY".equals(focus.marginStatus())) {
            advice.add(
                    isEnglish(language)
                            ? "Current margin supports bundling this item in value combos or off-peak promos."
                            : "Biên lợi nhuận tốt, phù hợp đưa món này vào Combo hoặc khuyến mãi khung giờ thấp điểm.");
        } else {
            advice.add(
                    isEnglish(language)
                            ? "Review portion weights on top-cost ingredients or negotiate bulk supplier rates."
                            : "Rà soát lại định lượng thành phần đắt nhất hoặc nhập sỉ để tối ưu giá vốn đơn vị.");
        }
        return advice;
    }

    private String buildRecipeContext(
            MenuItem menuItem,
            BigDecimal sellingPrice,
            List<SuggestedRecipeLine> currentLines,
            FoodCostAnalysis currentAnalysis,
            List<InventoryItem> activeInventory) {
        StringBuilder sb = new StringBuilder();
        sb.append("DISH: id=")
                .append(menuItem.getId())
                .append(" | name=")
                .append(menuItem.getName())
                .append(" | category=")
                .append(menuItem.getCategory() != null ? menuItem.getCategory().getName() : "N/A")
                .append(" | sellingPriceVND=")
                .append(sellingPrice)
                .append(" | description=")
                .append(menuItem.getDescription() != null ? menuItem.getDescription() : "")
                .append('\n');

        sb.append("CURRENT_ANALYSIS: totalCost=")
                .append(currentAnalysis.totalFoodCost())
                .append(", foodCostPercent=")
                .append(currentAnalysis.foodCostPercent())
                .append(", status=")
                .append(currentAnalysis.marginStatus())
                .append('\n');

        sb.append("CURRENT_BOM:\n");
        if (currentLines.isEmpty()) {
            sb.append("- NONE\n");
        } else {
            for (SuggestedRecipeLine line : currentLines) {
                sb.append("- inventoryItemId=")
                        .append(line.inventoryItemId())
                        .append(" | name=")
                        .append(line.inventoryItemName())
                        .append(" | qty=")
                        .append(line.quantityRequired())
                        .append(' ')
                        .append(line.unit())
                        .append(" | unitCost=")
                        .append(line.unitCost())
                        .append('\n');
            }
        }

        sb.append("ACTIVE_INVENTORY_ITEMS:\n");
        activeInventory.stream()
                .limit(MAX_INVENTORY_CONTEXT)
                .forEach(inv -> sb.append("- id=")
                        .append(inv.getId())
                        .append(" | name=")
                        .append(inv.getName())
                        .append(" | unit=")
                        .append(inv.getUnit())
                        .append(" | unitCost=")
                        .append(safeScale(inv.getUnitCost(), 2))
                        .append('\n'));
        return sb.toString();
    }

    private static SuggestedRecipeLine toLine(InventoryItem inv, BigDecimal qty, String reason) {
        BigDecimal unitCost = safeScale(inv.getUnitCost(), 2);
        BigDecimal normalizedQty = qty.setScale(3, RoundingMode.HALF_UP);
        BigDecimal lineCost = unitCost.multiply(normalizedQty).setScale(2, RoundingMode.HALF_UP);
        return new SuggestedRecipeLine(
                inv.getId(), inv.getName(), inv.getUnit(), normalizedQty, unitCost, lineCost, reason);
    }

    private static BigDecimal defaultQuantityForUnit(String unit) {
        if (unit == null) {
            return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
        }
        String normalized = unit.trim().toLowerCase();
        if ("g".equals(normalized) || "gr".equals(normalized) || "gram".equals(normalized) || "ml".equals(normalized)) {
            return new BigDecimal("100.000");
        }
        if ("kg".equals(normalized) || "l".equals(normalized) || "lit".equals(normalized)) {
            return new BigDecimal("0.150");
        }
        return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
    }

    private static String foldForMatching(String input) {
        return NoteSafetyAnalyzer.foldAccents(input == null ? "" : input);
    }

    private static BigDecimal safeScale(BigDecimal val, int scale) {
        return (val == null ? BigDecimal.ZERO : val).setScale(scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal readPositiveDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            BigDecimal value = node.isNumber()
                    ? node.decimalValue()
                    : new BigDecimal(node.asString("").trim());
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return value.setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
