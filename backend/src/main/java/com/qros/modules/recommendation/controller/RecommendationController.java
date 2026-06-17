package com.qros.modules.recommendation.controller;

import com.qros.modules.recommendation.dto.response.RecommendationResponse;
import com.qros.modules.recommendation.model.enums.RecommendationContext;
import com.qros.modules.recommendation.service.RecommendationService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/personalized")
    public ApiResponse<RecommendationResponse> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "ANY") RecommendationContext context,
            @RequestParam(defaultValue = "5")
                    @Min(value = 1, message = "Limit must be at least 1")
                    @Max(value = 20, message = "Limit cannot exceed 20")
                    int limit) {
        return ApiResponse.success(recommendationService.getPersonalizedRecommendations(context, limit));
    }

    @GetMapping("/popular")
    public ApiResponse<RecommendationResponse> getPopularItems(
            @RequestParam(defaultValue = "5")
                    @Min(value = 1, message = "Limit must be at least 1")
                    @Max(value = 20, message = "Limit cannot exceed 20")
                    int limit) {
        return ApiResponse.success(recommendationService.getPopularItems(limit));
    }

    @GetMapping("/cross-sell/{itemId}")
    public ApiResponse<RecommendationResponse> getCrossSellRecommendations(
            @PathVariable @Min(value = 1, message = "Item id must be positive") Long itemId,
            @RequestParam(defaultValue = "3")
                    @Min(value = 1, message = "Limit must be at least 1")
                    @Max(value = 20, message = "Limit cannot exceed 20")
                    int limit) {
        return ApiResponse.success(recommendationService.getCrossSellRecommendations(itemId, limit));
    }

    @GetMapping("/item/{itemId}")
    public ApiResponse<RecommendationResponse> getRecommendations(
            @PathVariable @Min(value = 1, message = "Item id must be positive") Long itemId,
            @RequestParam(defaultValue = "5")
                    @Min(value = 1, message = "Limit must be at least 1")
                    @Max(value = 20, message = "Limit cannot exceed 20")
                    int limit) {
        return ApiResponse.success(recommendationService.getRecommendations(itemId, limit));
    }
}
