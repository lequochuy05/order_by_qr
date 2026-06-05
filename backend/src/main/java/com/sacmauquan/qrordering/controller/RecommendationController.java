package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.CustomerPublicDto;
import com.sacmauquan.qrordering.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RecommendationController - Provides intelligent food recommendations based on
 * behavior and context.
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Retrieves personalized food recommendations based on time of day and item
     * popularity.
     * 
     * @param timeContext Optional time of day (e.g., Morning, Afternoon, Evening)
     * @param limit       Maximum number of items to return
     * @return List of recommended MenuItemResponse objects
     */
    @GetMapping("/personalized")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "Morning") String timeContext,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse
                .success(recommendationService.getPersonalizedRecommendations(timeContext, limit).stream()
                        .map(CustomerPublicDto::fromMenuItemResponse)
                        .toList());
    }

    /**
     * Retrieves cross-sell recommendations (items often bought together with the
     * given item).
     * 
     * @param itemId ID of the reference menu item
     * @param limit  Maximum number of items to return
     * @return List of cross-sell MenuItemResponse objects
     */
    @GetMapping("/cross-sell/{itemId}")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getCrossSellRecommendations(
            @PathVariable @NonNull Long itemId,
            @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.success(recommendationService.getCrossSellRecommendations(itemId, limit).stream()
                .map(CustomerPublicDto::fromMenuItemResponse)
                .toList());
    }

    /**
     * Retrieves the most popular food items in the system.
     * 
     * @param limit Maximum number of items to return
     * @return List of popular MenuItemResponse objects
     */
    @GetMapping("/popular")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getPopularItems(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getPopularItems(limit).stream()
                .map(CustomerPublicDto::fromMenuItemResponse)
                .toList());
    }

    /**
     * Retrieves similar items based on the category and attributes of the given
     * item ID.
     * 
     * @param itemId ID of the reference menu item
     * @param limit  Maximum number of items to return
     * @return List of similar MenuItemResponse objects
     */
    @GetMapping("/item/{itemId}")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getRecommendations(
            @PathVariable @NonNull Long itemId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getRecommendations(itemId, limit).stream()
                .map(CustomerPublicDto::fromMenuItemResponse)
                .toList());
    }
}
