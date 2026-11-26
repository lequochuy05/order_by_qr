package com.qros.modules.ai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AiRecipeAssistantResponse(
        Long menuItemId,
        String menuItemName,
        BigDecimal sellingPrice,
        String language,
        FoodCostAnalysis currentAnalysis,
        FoodCostAnalysis suggestedAnalysis,
        List<SuggestedRecipeLine> suggestedItems,
        List<String> costWarnings,
        List<String> pricingAdvice,
        LocalDateTime generatedAt,
        boolean aiGenerated) {

    public AiRecipeAssistantResponse {
        suggestedItems = suggestedItems == null ? List.of() : List.copyOf(suggestedItems);
        costWarnings = costWarnings == null ? List.of() : List.copyOf(costWarnings);
        pricingAdvice = pricingAdvice == null ? List.of() : List.copyOf(pricingAdvice);
    }

    public record FoodCostAnalysis(
            BigDecimal totalFoodCost,
            Double foodCostPercent,
            BigDecimal grossProfit,
            BigDecimal recommendedMinPrice,
            int missingUnitCostCount,
            String marginStatus) {}

    public record SuggestedRecipeLine(
            Long inventoryItemId,
            String inventoryItemName,
            String unit,
            BigDecimal quantityRequired,
            BigDecimal unitCost,
            BigDecimal lineCost,
            String reason) {}
}
