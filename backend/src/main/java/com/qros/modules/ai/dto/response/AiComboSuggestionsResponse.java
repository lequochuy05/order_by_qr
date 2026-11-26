package com.qros.modules.ai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AiComboSuggestionsResponse(
        String language,
        String summary,
        List<ComboProposal> suggestions,
        LocalDateTime generatedAt,
        boolean aiGenerated) {

    public AiComboSuggestionsResponse {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    public record ComboProposal(
            String name,
            String description,
            List<ComboItemProposal> items,
            BigDecimal originalTotalPrice,
            BigDecimal suggestedPrice,
            int discountPercent,
            String rationale) {

        public ComboProposal {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record ComboItemProposal(
            Long menuItemId, String name, BigDecimal unitPrice, int quantity, String categoryName) {}
}
