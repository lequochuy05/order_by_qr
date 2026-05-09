package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.MenuItemResponse;
import com.sacmauquan.qrordering.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RecommendationController - Cung cấp các gợi ý món ăn thông minh dựa trên hành vi và ngữ cảnh.
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Lấy gợi ý món ăn cá nhân hóa dựa trên ngữ cảnh thời gian và thời tiết.
     */
    @GetMapping("/personalized")
    public ApiResponse<List<MenuItemResponse>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "Morning") String timeContext,
            @RequestParam(defaultValue = "Clear") String weatherContext,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getPersonalizedRecommendations(timeContext, weatherContext, limit));
    }

    /**
     * Gợi ý các món ăn bán chéo (Cross-sell) thường được mua kèm với món ăn hiện tại.
     */
    @GetMapping("/cross-sell/{itemId}")
    public ApiResponse<List<MenuItemResponse>> getCrossSellRecommendations(
            @PathVariable @NonNull Long itemId,
            @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.success(recommendationService.getCrossSellRecommendations(itemId, limit));
    }

    /**
     * Lấy danh sách các món ăn phổ biến nhất (Popular Items) trong hệ thống.
     */
    @GetMapping("/popular")
    public ApiResponse<List<MenuItemResponse>> getPopularItems(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getPopularItems(limit));
    }

    /**
     * Lấy danh sách các món ăn tương tự (Similar Items) với món đang xem.
     */
    @GetMapping("/item/{itemId}")
    public ApiResponse<List<MenuItemResponse>> getRecommendations(
            @PathVariable @NonNull Long itemId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getRecommendations(itemId, limit));
    }
}
