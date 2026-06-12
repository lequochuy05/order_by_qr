package com.qros.modules.recommendation.dto.response;

import com.qros.modules.recommendation.model.enums.RecommendationType;

import java.math.BigDecimal;

public record RecommendationItemResponse(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        Long categoryId,
        String categoryName,
        RecommendationType type,
        String reason) {
}