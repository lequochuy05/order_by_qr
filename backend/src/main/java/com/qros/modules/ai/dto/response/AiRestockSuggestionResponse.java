package com.qros.modules.ai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AiRestockSuggestionResponse(
        int forecastDays,
        String language,
        String summary,
        int criticalCount,
        int highCount,
        BigDecimal totalEstimatedCost,
        List<String> recommendations,
        List<RestockItemProposal> items,
        LocalDateTime generatedAt,
        boolean aiGenerated) {

    public AiRestockSuggestionResponse {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record RestockItemProposal(
            Long inventoryItemId,
            String name,
            String unit,
            BigDecimal availableQuantity,
            BigDecimal lowStockThreshold,
            BigDecimal unitCost,
            BigDecimal projectedUsage,
            BigDecimal shortfall,
            BigDecimal suggestedRestockQty,
            BigDecimal estimatedCost,
            Double daysUntilStockout,
            String urgency,
            List<String> drivenByDishes,
            String reason) {

        public RestockItemProposal {
            drivenByDishes = drivenByDishes == null ? List.of() : List.copyOf(drivenByDishes);
        }
    }
}
