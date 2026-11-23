package com.qros.modules.ai.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AiPromotionSuggestionsResponse(
        String language,
        String summary,
        List<PromotionProposal> suggestions,
        LocalDateTime generatedAt,
        boolean aiGenerated) {

    public AiPromotionSuggestionsResponse {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    public record PromotionProposal(
            String name,
            double discountPercent,
            String startTime,
            String endTime,
            List<String> daysOfWeek,
            String rationale,
            String expectedImpact) {

        public PromotionProposal {
            daysOfWeek = daysOfWeek == null ? List.of() : List.copyOf(daysOfWeek);
        }
    }
}
