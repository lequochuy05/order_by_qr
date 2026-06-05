package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.CustomerPublicDto;
import com.sacmauquan.qrordering.service.CategoryService;
import com.sacmauquan.qrordering.service.ComboService;
import com.sacmauquan.qrordering.service.DiningTableService;
import com.sacmauquan.qrordering.service.MenuItemService;
import com.sacmauquan.qrordering.service.OrderService;
import com.sacmauquan.qrordering.service.RecommendationService;
import com.sacmauquan.qrordering.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class CustomerPublicController {

    private final CategoryService categoryService;
    private final MenuItemService menuItemService;
    private final ComboService comboService;
    private final DiningTableService tableService;
    private final OrderService orderService;
    private final RecommendationService recommendationService;
    private final SystemSettingsService settingsService;

    @GetMapping("/categories")
    public ApiResponse<List<CustomerPublicDto.CategoryItem>> getCategories() {
        return ApiResponse.success(categoryService.getPublicActive());
    }

    @GetMapping("/menu-items")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getMenuItems(
            @RequestParam(required = false) Integer categoryId) {
        if (categoryId != null) {
            return ApiResponse.success(menuItemService.getPublicItemsByCategory(categoryId));
        }
        return ApiResponse.success(menuItemService.getPublicMenuItems());
    }

    @GetMapping("/combos")
    public ApiResponse<List<CustomerPublicDto.ComboItem>> getCombos() {
        return ApiResponse.success(comboService.getPublicActive());
    }

    @GetMapping("/settings")
    public ApiResponse<CustomerPublicDto.Settings> getSettings() {
        return ApiResponse.success(settingsService.getPublicSettings());
    }

    @GetMapping("/tables/by-code/{tableCode}")
    public ApiResponse<CustomerPublicDto.Table> getTableByCode(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableService.getPublicByTableCode(tableCode));
    }

    @GetMapping("/tables/{tableId}/current-order")
    public ApiResponse<CustomerPublicDto.Order> getCurrentOrder(@PathVariable @NonNull Long tableId) {
        return orderService.getPublicCurrentOrderByTable(tableId)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }

    @GetMapping("/recommendations/personalized")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "Morning") String timeContext,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getPersonalizedRecommendations(timeContext, limit).stream()
                .map(CustomerPublicDto::fromMenuItemResponse)
                .toList());
    }

    @GetMapping("/recommendations/cross-sell/{itemId}")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getCrossSellRecommendations(
            @PathVariable @NonNull Long itemId,
            @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.success(recommendationService.getCrossSellRecommendations(itemId, limit).stream()
                .map(CustomerPublicDto::fromMenuItemResponse)
                .toList());
    }

    @GetMapping("/recommendations/popular")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getPopularItems(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getPopularItems(limit).stream()
                .map(CustomerPublicDto::fromMenuItemResponse)
                .toList());
    }

    @GetMapping("/recommendations/items/{itemId}")
    public ApiResponse<List<CustomerPublicDto.MenuItemItem>> getRecommendations(
            @PathVariable @NonNull Long itemId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getRecommendations(itemId, limit).stream()
                .map(CustomerPublicDto::fromMenuItemResponse)
                .toList());
    }
}
