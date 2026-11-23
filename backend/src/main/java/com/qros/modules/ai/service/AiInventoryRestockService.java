package com.qros.modules.ai.service;

import static com.qros.modules.ai.support.AiJsonExtractor.isEnglish;
import static com.qros.modules.ai.support.AiJsonExtractor.normalizeLanguage;
import static com.qros.modules.ai.support.AiJsonExtractor.readStringList;
import static com.qros.modules.ai.support.AiJsonExtractor.readText;
import static com.qros.modules.ai.support.AiJsonExtractor.truncate;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.request.AiRestockSuggestionRequest;
import com.qros.modules.ai.dto.response.AiRestockSuggestionResponse;
import com.qros.modules.ai.dto.response.AiRestockSuggestionResponse.RestockItemProposal;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import com.qros.modules.analytics.repository.projection.TopSellingItemProjection;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AiInventoryRestockService {

    private static final int DEFAULT_FORECAST_DAYS = 7;
    private static final int DEMAND_LOOKBACK_DAYS = 30;
    private static final BigDecimal SAFETY_MULTIPLIER = new BigDecimal("1.10");
    private static final int MAX_DRIVEN_DISHES = 3;
    private static final int MAX_PROMPT_ITEMS = 25;
    private static final int MAX_RECOMMENDATIONS = 3;

    private final InventoryItemRepository inventoryItemRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final AnalyticsQueryRepository analyticsQueryRepository;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiInventoryRestockService(
            InventoryItemRepository inventoryItemRepository,
            RecipeItemRepository recipeItemRepository,
            AnalyticsQueryRepository analyticsQueryRepository,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.recipeItemRepository = recipeItemRepository;
        this.analyticsQueryRepository = analyticsQueryRepository;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "inventory-restock")
                .description("AI inventory restock forecast requests")
                .register(meterRegistry);
    }

    public AiRestockSuggestionResponse suggestRestock(AiRestockSuggestionRequest request) {
        LocalDateTime now = AppTime.now();
        LocalDate today = now.toLocalDate();
        int forecastDays =
                request != null && request.forecastDays() != null ? request.forecastDays() : DEFAULT_FORECAST_DAYS;
        String language = normalizeLanguage(request != null ? request.language() : null);

        List<InventoryItem> activeItems = inventoryItemRepository.findByActiveTrueOrderByNameAsc();
        if (activeItems.isEmpty()) {
            String emptySummary = isEnglish(language)
                    ? "No active inventory items found to forecast."
                    : "Chưa có nguyên liệu đang hoạt động để dự báo nhập kho.";
            return new AiRestockSuggestionResponse(
                    forecastDays,
                    language,
                    emptySummary,
                    0,
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    List.of(),
                    List.of(),
                    now,
                    false);
        }

        List<TopSellingItemProjection> topSellers = analyticsQueryRepository.topSellingItems(
                today.minusDays(DEMAND_LOOKBACK_DAYS - 1L), today.plusDays(1), 50);
        Map<Long, Long> sold30dByMenuId = new HashMap<>();
        for (TopSellingItemProjection row : topSellers) {
            if (row.getMenuItemId() != null && row.getQuantitySold() != null) {
                sold30dByMenuId.merge(row.getMenuItemId(), row.getQuantitySold(), Long::sum);
            }
        }

        List<RecipeItem> activeRecipes = recipeItemRepository.findAllActiveWithMenuAndInventory();
        Map<Long, BigDecimal> dailyUsageByInventoryId = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> dishDemandByInventoryId = new HashMap<>();

        for (RecipeItem recipe : activeRecipes) {
            if (recipe.getMenuItem() == null
                    || recipe.getInventoryItem() == null
                    || recipe.getQuantityRequired() == null) {
                continue;
            }
            Long menuId = recipe.getMenuItem().getId();
            Long invId = recipe.getInventoryItem().getId();
            long sold30d = sold30dByMenuId.getOrDefault(menuId, 0L);
            if (sold30d <= 0) {
                continue;
            }
            BigDecimal dailyDishRate = BigDecimal.valueOf(sold30d)
                    .divide(BigDecimal.valueOf(DEMAND_LOOKBACK_DAYS), 6, RoundingMode.HALF_UP);
            BigDecimal dailyIngredientUsage = dailyDishRate.multiply(recipe.getQuantityRequired());
            dailyUsageByInventoryId.merge(invId, dailyIngredientUsage, BigDecimal::add);

            BigDecimal projectedDishIngredient = dailyIngredientUsage
                    .multiply(BigDecimal.valueOf(forecastDays))
                    .multiply(SAFETY_MULTIPLIER);
            dishDemandByInventoryId
                    .computeIfAbsent(invId, k -> new LinkedHashMap<>())
                    .merge(recipe.getMenuItem().getName(), projectedDishIngredient, BigDecimal::add);
        }

        List<EvaluatedItem> evaluated = new ArrayList<>();
        for (InventoryItem item : activeItems) {
            BigDecimal available = item.availableQuantity();
            BigDecimal threshold = safeScale(item.getLowStockThreshold(), 3);
            BigDecimal unitCost = safeScale(item.getUnitCost(), 2);
            BigDecimal dailyUsage = dailyUsageByInventoryId.getOrDefault(item.getId(), BigDecimal.ZERO);

            BigDecimal projectedUsage = dailyUsage
                    .multiply(BigDecimal.valueOf(forecastDays))
                    .multiply(SAFETY_MULTIPLIER)
                    .setScale(3, RoundingMode.HALF_UP);
            BigDecimal shortfall =
                    projectedUsage.subtract(available).max(BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
            BigDecimal thresholdGap = threshold.subtract(available).max(BigDecimal.ZERO);
            BigDecimal suggestedRestockQty = shortfall.max(thresholdGap).setScale(3, RoundingMode.HALF_UP);
            BigDecimal estimatedCost = suggestedRestockQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

            Double daysUntilStockout = null;
            if (dailyUsage.compareTo(BigDecimal.ZERO) > 0) {
                daysUntilStockout =
                        available.divide(dailyUsage, 1, RoundingMode.HALF_UP).doubleValue();
            }

            String urgency = resolveUrgency(available, daysUntilStockout, shortfall, item.isLowStock());
            List<String> drivenByDishes =
                    dishDemandByInventoryId.getOrDefault(item.getId(), Map.of()).entrySet().stream()
                            .sorted(Map.Entry.<String, BigDecimal>comparingByValue()
                                    .reversed())
                            .limit(MAX_DRIVEN_DISHES)
                            .map(Map.Entry::getKey)
                            .toList();

            String defaultReason = buildDeterministicReason(
                    urgency, available, projectedUsage, daysUntilStockout, threshold, item.getUnit(), language);

            evaluated.add(new EvaluatedItem(
                    new RestockItemProposal(
                            item.getId(),
                            item.getName(),
                            item.getUnit(),
                            available,
                            threshold,
                            unitCost,
                            projectedUsage,
                            shortfall,
                            suggestedRestockQty,
                            estimatedCost,
                            daysUntilStockout,
                            urgency,
                            drivenByDishes,
                            defaultReason),
                    urgencyRank(urgency)));
        }

        evaluated.sort(Comparator.comparingInt(EvaluatedItem::rank)
                .reversed()
                .thenComparing((EvaluatedItem e) -> e.proposal().shortfall(), Comparator.reverseOrder())
                .thenComparing(e ->
                        e.proposal().daysUntilStockout() != null ? e.proposal().daysUntilStockout() : Double.MAX_VALUE)
                .thenComparing(e -> e.proposal().name(), String.CASE_INSENSITIVE_ORDER));

        List<RestockItemProposal> proposals =
                evaluated.stream().map(EvaluatedItem::proposal).toList();
        int criticalCount = (int)
                proposals.stream().filter(p -> "CRITICAL".equals(p.urgency())).count();
        int highCount =
                (int) proposals.stream().filter(p -> "HIGH".equals(p.urgency())).count();
        BigDecimal totalEstimatedCost = proposals.stream()
                .filter(p -> p.suggestedRestockQty().compareTo(BigDecimal.ZERO) > 0)
                .map(RestockItemProposal::estimatedCost)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);

        String restockContext =
                buildContextString(forecastDays, criticalCount, highCount, totalEstimatedCost, proposals);
        String systemPrompt = promptBuilder.buildRestockPrompt(restockContext, forecastDays, language);
        String rawReply = aiChatGateway.chat(
                new AiChatRequest("Forecast inventory restock for next %d days.".formatted(forecastDays), List.of()),
                systemPrompt,
                "");
        aiRequestsCounter.increment();

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildFallbackResponse(
                    forecastDays, language, criticalCount, highCount, totalEstimatedCost, proposals, now);
        }

        AiRestockSuggestionResponse parsed = tryParseAiResponse(
                forecastDays, language, criticalCount, highCount, totalEstimatedCost, proposals, rawReply, now);
        return parsed != null
                ? parsed
                : buildFallbackResponse(
                        forecastDays, language, criticalCount, highCount, totalEstimatedCost, proposals, now);
    }

    private AiRestockSuggestionResponse tryParseAiResponse(
            int forecastDays,
            String language,
            int criticalCount,
            int highCount,
            BigDecimal totalEstimatedCost,
            List<RestockItemProposal> baseProposals,
            String rawReply,
            LocalDateTime generatedAt) {
        String extractedJson = AiJsonExtractor.extractJsonObject(rawReply);
        if (extractedJson == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extractedJson);
            String summary = readText(root.path("summary"), "");
            List<String> recommendations = readStringList(root.path("recommendations"), MAX_RECOMMENDATIONS);

            Map<Long, String> aiReasons = new HashMap<>();
            JsonNode reasonsNode = root.path("itemReasons");
            if (reasonsNode.isArray()) {
                for (JsonNode entry : reasonsNode) {
                    long id = entry.path("inventoryItemId").asLong(-1L);
                    String reason = readText(entry.path("reason"), "");
                    if (id > 0 && !reason.isBlank()) {
                        aiReasons.putIfAbsent(id, truncate(reason, 120));
                    }
                }
            }

            if (summary.isBlank() && recommendations.isEmpty() && aiReasons.isEmpty()) {
                return null;
            }

            List<RestockItemProposal> mergedProposals = baseProposals.stream()
                    .map(p -> {
                        String overrideReason = aiReasons.get(p.inventoryItemId());
                        if (overrideReason == null || overrideReason.isBlank()) {
                            return p;
                        }
                        return new RestockItemProposal(
                                p.inventoryItemId(),
                                p.name(),
                                p.unit(),
                                p.availableQuantity(),
                                p.lowStockThreshold(),
                                p.unitCost(),
                                p.projectedUsage(),
                                p.shortfall(),
                                p.suggestedRestockQty(),
                                p.estimatedCost(),
                                p.daysUntilStockout(),
                                p.urgency(),
                                p.drivenByDishes(),
                                overrideReason);
                    })
                    .toList();

            String finalSummary = summary.isBlank()
                    ? buildDefaultSummary(forecastDays, language, criticalCount, highCount, mergedProposals.size())
                    : summary;
            List<String> finalRecommendations = recommendations.isEmpty()
                    ? buildDefaultRecommendations(forecastDays, language, mergedProposals)
                    : recommendations;

            return new AiRestockSuggestionResponse(
                    forecastDays,
                    language,
                    finalSummary,
                    criticalCount,
                    highCount,
                    totalEstimatedCost,
                    finalRecommendations,
                    mergedProposals,
                    generatedAt,
                    true);
        } catch (Exception ex) {
            log.debug("Failed to parse AI restock JSON payload: {}", ex.getMessage());
            return null;
        }
    }

    private AiRestockSuggestionResponse buildFallbackResponse(
            int forecastDays,
            String language,
            int criticalCount,
            int highCount,
            BigDecimal totalEstimatedCost,
            List<RestockItemProposal> proposals,
            LocalDateTime generatedAt) {
        return new AiRestockSuggestionResponse(
                forecastDays,
                language,
                buildDefaultSummary(forecastDays, language, criticalCount, highCount, proposals.size()),
                criticalCount,
                highCount,
                totalEstimatedCost,
                buildDefaultRecommendations(forecastDays, language, proposals),
                proposals,
                generatedAt,
                false);
    }

    private String buildDefaultSummary(
            int forecastDays, String language, int criticalCount, int highCount, int totalItems) {
        if (isEnglish(language)) {
            if (criticalCount == 0 && highCount == 0) {
                return "Stock across %d active ingredients looks stable for the next %d days."
                        .formatted(totalItems, forecastDays);
            }
            return "Forecast over %d days flags %d critical and %d high-priority ingredients needing restock."
                    .formatted(forecastDays, criticalCount, highCount);
        }
        if (criticalCount == 0 && highCount == 0) {
            return "Tồn kho của %d nguyên liệu hiện đủ đáp ứng nhu cầu dự kiến trong %d ngày tới."
                    .formatted(totalItems, forecastDays);
        }
        return "Dự báo %d ngày tới ghi nhận %d nguyên liệu khẩn cấp và %d nguyên liệu thiếu hụt cần nhập thêm."
                .formatted(forecastDays, criticalCount, highCount);
    }

    private List<String> buildDefaultRecommendations(
            int forecastDays, String language, List<RestockItemProposal> proposals) {
        List<String> recs = new ArrayList<>();
        List<RestockItemProposal> urgent = proposals.stream()
                .filter(p -> "CRITICAL".equals(p.urgency()) || "HIGH".equals(p.urgency()))
                .limit(2)
                .toList();
        for (RestockItemProposal item : urgent) {
            if (isEnglish(language)) {
                recs.add("Restock %s: suggested +%s %s to cover %d-day BOM demand."
                        .formatted(
                                item.name(),
                                item.suggestedRestockQty().stripTrailingZeros().toPlainString(),
                                item.unit(),
                                forecastDays));
            } else {
                recs.add("Ưu tiên nhập %s: đề xuất +%s %s để đáp ứng định mức %d ngày."
                        .formatted(
                                item.name(),
                                item.suggestedRestockQty().stripTrailingZeros().toPlainString(),
                                item.unit(),
                                forecastDays));
            }
        }
        if (recs.isEmpty()) {
            recs.add(
                    isEnglish(language)
                            ? "Maintain regular cycle counts and keep supplier lead times updated."
                            : "Duy trì kiểm kho định kỳ và rà soát ngưỡng cảnh báo cho các món bán chạy.");
        }
        return recs;
    }

    private String buildDeterministicReason(
            String urgency,
            BigDecimal available,
            BigDecimal projectedUsage,
            Double daysUntilStockout,
            BigDecimal threshold,
            String unit,
            String language) {
        String availStr = available.stripTrailingZeros().toPlainString();
        String projStr = projectedUsage.stripTrailingZeros().toPlainString();
        if (isEnglish(language)) {
            return switch (urgency) {
                case "CRITICAL" ->
                    available.compareTo(BigDecimal.ZERO) <= 0
                            ? "Out of stock (0 %s available)".formatted(unit)
                            : "Covers only ~%s days vs forecast %s %s".formatted(daysUntilStockout, projStr, unit);
                case "HIGH" -> "Available %s %s < BOM forecast %s %s".formatted(availStr, unit, projStr, unit);
                case "MEDIUM" ->
                    "At or below low-stock threshold (%s %s)"
                            .formatted(threshold.stripTrailingZeros().toPlainString(), unit);
                default -> "Sufficient for forecast demand (%s %s available)".formatted(availStr, unit);
            };
        }
        return switch (urgency) {
            case "CRITICAL" ->
                available.compareTo(BigDecimal.ZERO) <= 0
                        ? "Đã hết hàng khả dụng (0 %s)".formatted(unit)
                        : "Chỉ đủ dùng khoảng %s ngày (dự báo cần %s %s)".formatted(daysUntilStockout, projStr, unit);
            case "HIGH" -> "Tồn %s %s thấp hơn dự báo tiêu thụ %s %s".formatted(availStr, unit, projStr, unit);
            case "MEDIUM" ->
                "Chạm hoặc dưới ngưỡng cảnh báo (%s %s)"
                        .formatted(threshold.stripTrailingZeros().toPlainString(), unit);
            default -> "Đủ đáp ứng dự báo (%s %s khả dụng)".formatted(availStr, unit);
        };
    }

    private String buildContextString(
            int forecastDays,
            int criticalCount,
            int highCount,
            BigDecimal totalEstimatedCost,
            List<RestockItemProposal> proposals) {
        StringBuilder sb = new StringBuilder();
        sb.append("FORECAST_DAYS: ").append(forecastDays).append('\n');
        sb.append("CRITICAL_COUNT: ")
                .append(criticalCount)
                .append(", HIGH_COUNT: ")
                .append(highCount)
                .append(", TOTAL_EST_COST_VND: ")
                .append(totalEstimatedCost)
                .append('\n');
        sb.append("PRIORITY_ITEMS:\n");
        proposals.stream()
                .limit(MAX_PROMPT_ITEMS)
                .forEach(item -> sb.append("- id=")
                        .append(item.inventoryItemId())
                        .append(" | name=")
                        .append(item.name())
                        .append(" | unit=")
                        .append(item.unit())
                        .append(" | available=")
                        .append(item.availableQuantity())
                        .append(" | threshold=")
                        .append(item.lowStockThreshold())
                        .append(" | projected=")
                        .append(item.projectedUsage())
                        .append(" | shortfall=")
                        .append(item.shortfall())
                        .append(" | suggestedQty=")
                        .append(item.suggestedRestockQty())
                        .append(" | daysLeft=")
                        .append(item.daysUntilStockout() != null ? item.daysUntilStockout() : "N/A")
                        .append(" | urgency=")
                        .append(item.urgency())
                        .append(" | drivenBy=")
                        .append(String.join(", ", item.drivenByDishes()))
                        .append('\n'));
        return sb.toString();
    }

    private static String resolveUrgency(
            BigDecimal available, Double daysUntilStockout, BigDecimal shortfall, boolean isLowStock) {
        if (available.compareTo(BigDecimal.ZERO) <= 0 || (daysUntilStockout != null && daysUntilStockout < 2.0)) {
            return "CRITICAL";
        }
        if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
            return "HIGH";
        }
        if (isLowStock) {
            return "MEDIUM";
        }
        return "OK";
    }

    private static int urgencyRank(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private static BigDecimal safeScale(BigDecimal val, int scale) {
        return (val == null ? BigDecimal.ZERO : val).setScale(scale, RoundingMode.HALF_UP);
    }

    private record EvaluatedItem(RestockItemProposal proposal, int rank) {}
}
