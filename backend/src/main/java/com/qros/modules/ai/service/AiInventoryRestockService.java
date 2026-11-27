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
import com.qros.modules.ai.service.RestockForecastCalculator.ForecastEvaluation;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.dto.projection.MenuItemDemandProjection;
import com.qros.modules.analytics.service.AnalyticsLookupService;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.service.InventoryLookupService;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    private static final int MAX_RECOMMENDATIONS = 4;

    private final InventoryLookupService inventoryLookupService;
    private final AnalyticsLookupService analyticsLookupService;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiInventoryRestockService(
            InventoryLookupService inventoryLookupService,
            AnalyticsLookupService analyticsLookupService,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.inventoryLookupService = inventoryLookupService;
        this.analyticsLookupService = analyticsLookupService;
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

        List<InventoryItem> activeItems = inventoryLookupService.findActiveInventoryItemsOrdered();
        if (activeItems.isEmpty()) {
            return buildEmptyResponse(forecastDays, language, now);
        }

        List<MenuItemDemandProjection> menuDemand = analyticsLookupService.menuItemDemandIncludingCombos(
                today.minusDays(RestockForecastCalculator.DEMAND_LOOKBACK_DAYS), today);
        List<RecipeItem> activeRecipes = inventoryLookupService.findAllActiveRecipesWithMenuAndInventory();

        ForecastEvaluation evaluation = RestockForecastCalculator.evaluateFromMenuDemand(
                activeItems, menuDemand, activeRecipes, forecastDays, language);

        String restockContext = RestockForecastCalculator.buildContextString(forecastDays, evaluation);
        String systemPrompt = promptBuilder.buildRestockPrompt(restockContext, forecastDays, language);
        String rawReply = aiChatGateway.chat(
                new AiChatRequest("Forecast inventory restock for next %d days.".formatted(forecastDays), List.of()),
                systemPrompt,
                "");
        aiRequestsCounter.increment();

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildFallbackResponse(forecastDays, language, evaluation, now);
        }

        AiRestockSuggestionResponse parsed = tryParseAiResponse(forecastDays, language, evaluation, rawReply, now);
        return parsed != null ? parsed : buildFallbackResponse(forecastDays, language, evaluation, now);
    }

    private AiRestockSuggestionResponse tryParseAiResponse(
            int forecastDays,
            String language,
            ForecastEvaluation evaluation,
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
            Map<Long, String> aiReasons = extractAiReasons(root.path("itemReasons"));

            if (summary.isBlank() && recommendations.isEmpty() && aiReasons.isEmpty()) {
                return null;
            }

            List<RestockItemProposal> mergedProposals = mergeProposalReasons(evaluation.proposals(), aiReasons);
            String finalSummary = summary.isBlank()
                    ? RestockForecastCalculator.buildDefaultSummary(
                            forecastDays,
                            language,
                            evaluation.criticalCount(),
                            evaluation.highCount(),
                            mergedProposals.size())
                    : summary;
            List<String> finalRecommendations = recommendations.isEmpty()
                    ? RestockForecastCalculator.buildDefaultRecommendations(forecastDays, language, mergedProposals)
                    : recommendations;

            return new AiRestockSuggestionResponse(
                    forecastDays,
                    language,
                    finalSummary,
                    evaluation.criticalCount(),
                    evaluation.highCount(),
                    evaluation.totalEstimatedCost(),
                    finalRecommendations,
                    mergedProposals,
                    generatedAt,
                    true);
        } catch (Exception ex) {
            log.debug("Failed to parse AI restock JSON payload: {}", ex.getMessage());
            return null;
        }
    }

    private static Map<Long, String> extractAiReasons(JsonNode reasonsNode) {
        Map<Long, String> aiReasons = new HashMap<>();
        if (reasonsNode == null || !reasonsNode.isArray()) {
            return aiReasons;
        }
        for (JsonNode entry : reasonsNode) {
            long id = entry.path("inventoryItemId").asLong(-1L);
            String reason = readText(entry.path("reason"), "");
            if (id > 0 && !reason.isBlank()) {
                aiReasons.putIfAbsent(id, truncate(reason, 120));
            }
        }
        return aiReasons;
    }

    private static List<RestockItemProposal> mergeProposalReasons(
            List<RestockItemProposal> baseProposals, Map<Long, String> aiReasons) {
        return baseProposals.stream()
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
    }

    private static AiRestockSuggestionResponse buildEmptyResponse(
            int forecastDays, String language, LocalDateTime now) {
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

    private static AiRestockSuggestionResponse buildFallbackResponse(
            int forecastDays, String language, ForecastEvaluation evaluation, LocalDateTime generatedAt) {
        return new AiRestockSuggestionResponse(
                forecastDays,
                language,
                RestockForecastCalculator.buildDefaultSummary(
                        forecastDays,
                        language,
                        evaluation.criticalCount(),
                        evaluation.highCount(),
                        evaluation.proposals().size()),
                evaluation.criticalCount(),
                evaluation.highCount(),
                evaluation.totalEstimatedCost(),
                RestockForecastCalculator.buildDefaultRecommendations(forecastDays, language, evaluation.proposals()),
                evaluation.proposals(),
                generatedAt,
                false);
    }
}
