package com.qros.modules.recommendation.dto.response;

import com.qros.modules.recommendation.model.enums.RecommendationContext;
import com.qros.modules.recommendation.model.enums.RecommendationType;

import java.util.List;

public record RecommendationResponse(
        RecommendationType type,
        RecommendationContext context,
        int limit,
        List<RecommendationItemResponse> items) {
}