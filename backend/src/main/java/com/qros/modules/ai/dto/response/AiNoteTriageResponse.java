package com.qros.modules.ai.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AiNoteTriageResponse(
        String language,
        String summary,
        int criticalCount,
        int highCount,
        List<TriagedNoteAlert> alerts,
        LocalDateTime generatedAt,
        boolean aiGenerated) {

    public AiNoteTriageResponse {
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record TriagedNoteAlert(
            String sourceType,
            Long sourceId,
            String tableLabel,
            String itemName,
            String rawNote,
            String riskLevel,
            List<String> allergens,
            List<String> urgencyFlags,
            List<String> dietaryFlags,
            String actionInstruction,
            long waitMinutes) {

        public TriagedNoteAlert {
            allergens = allergens == null ? List.of() : List.copyOf(allergens);
            urgencyFlags = urgencyFlags == null ? List.of() : List.copyOf(urgencyFlags);
            dietaryFlags = dietaryFlags == null ? List.of() : List.copyOf(dietaryFlags);
        }
    }
}
