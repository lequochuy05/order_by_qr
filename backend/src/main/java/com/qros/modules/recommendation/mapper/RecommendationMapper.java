package com.qros.modules.recommendation.mapper;

import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.recommendation.dto.response.RecommendationItemResponse;
import com.qros.modules.recommendation.dto.response.RecommendationResponse;
import com.qros.modules.recommendation.model.enums.RecommendationContext;
import com.qros.modules.recommendation.model.enums.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecommendationMapper {

    public RecommendationItemResponse toItemResponse(
            MenuItem item,
            RecommendationType type,
            String reason) {
        Category category = item.getCategory();

        return new RecommendationItemResponse(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getImg(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                type,
                reason);
    }

    public List<RecommendationItemResponse> toItemResponses(
            List<MenuItem> items,
            RecommendationType type,
            String reason) {
        return items.stream()
                .map(item -> toItemResponse(item, type, reason))
                .toList();
    }

    public RecommendationResponse toResponse(
            RecommendationType type,
            RecommendationContext context,
            int limit,
            List<MenuItem> items,
            String reason) {
        return new RecommendationResponse(
                type,
                context,
                limit,
                toItemResponses(items, type, reason));
    }
}
