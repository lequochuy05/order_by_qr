package com.qros.modules.ai.service;

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
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.service.InventoryLookupService;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private static final int MAX_SUGGESTED_ITEMS = 8;

    private final MenuCatalogLookupService menuCatalogLookupService;
    private final InventoryLookupService inventoryLookupService;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiRecipeAssistantService(
            MenuCatalogLookupService menuCatalogLookupService,
            InventoryLookupService inventoryLookupService,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.menuCatalogLookupService = menuCatalogLookupService;
        this.inventoryLookupService = inventoryLookupService;
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

        MenuItem menuItem = menuCatalogLookupService
                .findMenuItemById(request.menuItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));

        BigDecimal sellingPrice = AiJsonExtractor.safeScale(menuItem.getPrice(), 2);
        List<InventoryItem> activeInventory = inventoryLookupService.findActiveInventoryItemsOrdered();
        Map<Long, InventoryItem> activeInventoryById = activeInventory.stream()
                .collect(Collectors.toMap(
                        InventoryItem::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<SuggestedRecipeLine> currentLines = resolveCurrentLines(request, activeInventoryById);
        FoodCostAnalysis currentAnalysis = computeFoodCostAnalysis(sellingPrice, currentLines);

        String context = RecipeFoodCostAnalyzer.buildRecipeContext(
                menuItem, sellingPrice, currentLines, currentAnalysis, activeInventory);
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

    static FoodCostAnalysis computeFoodCostAnalysis(BigDecimal sellingPrice, List<SuggestedRecipeLine> lines) {
        return RecipeFoodCostAnalyzer.computeFoodCostAnalysis(sellingPrice, lines);
    }

    private List<SuggestedRecipeLine> resolveCurrentLines(
            AiRecipeAssistantRequest request, Map<Long, InventoryItem> activeInventoryById) {
        if (request.currentItems() != null && !request.currentItems().isEmpty()) {
            return resolveDraftLines(request.currentItems(), activeInventoryById);
        }

        List<RecipeItem> savedRecipes = inventoryLookupService.findRecipesByMenuItemId(request.menuItemId());
        return savedRecipes.stream()
                .filter(r -> r.getInventoryItem() != null
                        && r.getQuantityRequired() != null
                        && r.getQuantityRequired().compareTo(BigDecimal.ZERO) > 0)
                .map(r -> RecipeFoodCostAnalyzer.toLine(
                        r.getInventoryItem(), r.getQuantityRequired().setScale(3, RoundingMode.HALF_UP), ""))
                .toList();
    }

    private List<SuggestedRecipeLine> resolveDraftLines(
            List<DraftRecipeLine> draftLines, Map<Long, InventoryItem> activeInventoryById) {
        List<Long> missingIds = draftLines.stream()
                .map(DraftRecipeLine::inventoryItemId)
                .filter(id -> id != null && !activeInventoryById.containsKey(id))
                .distinct()
                .toList();
        Map<Long, InventoryItem> combinedMap = new LinkedHashMap<>(activeInventoryById);
        if (!missingIds.isEmpty()) {
            inventoryLookupService
                    .findInventoryItemsByIds(missingIds)
                    .forEach(item -> combinedMap.putIfAbsent(item.getId(), item));
        }

        Map<Long, SuggestedRecipeLine> deduped = new LinkedHashMap<>();
        for (DraftRecipeLine line : draftLines) {
            if (line.inventoryItemId() == null
                    || line.quantityRequired() == null
                    || line.quantityRequired().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            InventoryItem inv = combinedMap.get(line.inventoryItemId());
            if (inv != null) {
                BigDecimal qty = line.quantityRequired().setScale(3, RoundingMode.HALF_UP);
                deduped.putIfAbsent(inv.getId(), RecipeFoodCostAnalyzer.toLine(inv, qty, ""));
            }
        }
        return List.copyOf(deduped.values());
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
            List<SuggestedRecipeLine> suggested = parseSuggestedLines(root.path("suggestedItems"), activeInventoryById);
            List<String> costWarnings = readStringList(root.path("costWarnings"), 4);
            List<String> pricingAdvice = readStringList(root.path("pricingAdvice"), 4);

            boolean aiGenerated = !suggested.isEmpty();
            List<SuggestedRecipeLine> effectiveLines = aiGenerated
                    ? suggested
                    : (!currentLines.isEmpty()
                            ? currentLines
                            : RecipeFoodCostAnalyzer.matchIngredientsByName(
                                    menuItem.getName(), activeInventory, language));

            if (!aiGenerated && effectiveLines.isEmpty() && costWarnings.isEmpty() && pricingAdvice.isEmpty()) {
                return null;
            }

            FoodCostAnalysis effectiveAnalysis = computeFoodCostAnalysis(sellingPrice, effectiveLines);
            return new AiRecipeAssistantResponse(
                    menuItem.getId(),
                    menuItem.getName(),
                    sellingPrice,
                    language,
                    currentAnalysis,
                    effectiveAnalysis,
                    effectiveLines,
                    costWarnings.isEmpty()
                            ? RecipeFoodCostAnalyzer.buildDefaultCostWarnings(
                                    sellingPrice, currentAnalysis, effectiveAnalysis, language)
                            : costWarnings,
                    pricingAdvice.isEmpty()
                            ? RecipeFoodCostAnalyzer.buildDefaultPricingAdvice(
                                    sellingPrice, currentAnalysis, effectiveAnalysis, language)
                            : pricingAdvice,
                    generatedAt,
                    aiGenerated);
        } catch (Exception ex) {
            log.debug("Failed to parse AI recipe assistant JSON payload: {}", ex.getMessage());
            return null;
        }
    }

    private static List<SuggestedRecipeLine> parseSuggestedLines(
            JsonNode itemsNode, Map<Long, InventoryItem> activeInventoryById) {
        List<SuggestedRecipeLine> suggested = new ArrayList<>();
        if (itemsNode == null || !itemsNode.isArray()) {
            return suggested;
        }
        Set<Long> seenIds = new LinkedHashSet<>();
        for (JsonNode entry : itemsNode) {
            long invId = entry.path("inventoryItemId").asLong(-1L);
            InventoryItem inv = activeInventoryById.get(invId);
            if (inv == null || !seenIds.add(invId)) {
                continue;
            }
            BigDecimal qty = AiJsonExtractor.readPositiveDecimal(entry.path("quantityRequired"), 3);
            if (qty == null) {
                continue;
            }
            String reason = readText(entry.path("reason"), "");
            suggested.add(RecipeFoodCostAnalyzer.toLine(inv, qty, truncate(reason, 120)));
            if (suggested.size() >= MAX_SUGGESTED_ITEMS) {
                break;
            }
        }
        return suggested;
    }

    private static AiRecipeAssistantResponse buildDeterministicFallback(
            MenuItem menuItem,
            BigDecimal sellingPrice,
            String language,
            List<SuggestedRecipeLine> currentLines,
            FoodCostAnalysis currentAnalysis,
            List<InventoryItem> activeInventory,
            LocalDateTime generatedAt) {
        List<SuggestedRecipeLine> suggestedLines = !currentLines.isEmpty()
                ? RecipeFoodCostAnalyzer.annotateExistingLines(currentLines, language)
                : RecipeFoodCostAnalyzer.matchIngredientsByName(menuItem.getName(), activeInventory, language);
        FoodCostAnalysis suggestedAnalysis = computeFoodCostAnalysis(sellingPrice, suggestedLines);

        return new AiRecipeAssistantResponse(
                menuItem.getId(),
                menuItem.getName(),
                sellingPrice,
                language,
                currentAnalysis,
                suggestedAnalysis,
                suggestedLines,
                RecipeFoodCostAnalyzer.buildDefaultCostWarnings(
                        sellingPrice, currentAnalysis, suggestedAnalysis, language),
                RecipeFoodCostAnalyzer.buildDefaultPricingAdvice(
                        sellingPrice, currentAnalysis, suggestedAnalysis, language),
                generatedAt,
                false);
    }
}
