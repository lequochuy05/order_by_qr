package com.qros.modules.menu.controller;

import com.qros.shared.response.ApiResponse;
import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.menu.service.CategoryService;
import com.qros.modules.menu.service.ComboService;
import com.qros.modules.table.service.DiningTableService;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.modules.recomendation.service.RecommendationService;
import com.qros.modules.settings.service.SystemSettingsService;
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
public class PublicMenuController {

    private final CategoryService categoryService;
    private final MenuItemService menuItemService;
    private final ComboService comboService;
    private final DiningTableService tableService;
    private final RecommendationService recommendationService;
    private final SystemSettingsService settingsService;

    @GetMapping("/categories")
    public ApiResponse<List<PublicMenuResponse.CategoryItem>> getCategories() {
        return ApiResponse.success(categoryService.getPublicActive());
    }

    @GetMapping("/menu-items")
    public ApiResponse<List<PublicMenuResponse.MenuItemItem>> getMenuItems(
            @RequestParam(required = false) Integer categoryId) {
        if (categoryId != null) {
            return ApiResponse.success(menuItemService.getPublicItemsByCategory(categoryId));
        }
        return ApiResponse.success(menuItemService.getPublicMenuItems());
    }

    @GetMapping("/combos")
    public ApiResponse<List<PublicMenuResponse.ComboItem>> getCombos() {
        return ApiResponse.success(comboService.getPublicActive());
    }

    @GetMapping("/settings")
    public ApiResponse<PublicMenuResponse.Settings> getSettings() {
        return ApiResponse.success(settingsService.getPublicSettings());
    }

    @GetMapping("/tables/by-code/{tableCode}")
    public ApiResponse<PublicMenuResponse.Table> getTableByCode(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableService.getPublicByTableCode(tableCode));
    }

    @GetMapping("/recommendations/personalized")
    public ApiResponse<List<PublicMenuResponse.MenuItemItem>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "Morning") String timeContext,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getPersonalizedRecommendations(timeContext, limit).stream()
                .map(PublicMenuResponse::fromMenuItemResponse)
                .toList());
    }

    @GetMapping("/recommendations/cross-sell/{itemId}")
    public ApiResponse<List<PublicMenuResponse.MenuItemItem>> getCrossSellRecommendations(
            @PathVariable @NonNull Long itemId,
            @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.success(recommendationService.getCrossSellRecommendations(itemId, limit).stream()
                .map(PublicMenuResponse::fromMenuItemResponse)
                .toList());
    }

    @GetMapping("/recommendations/popular")
    public ApiResponse<List<PublicMenuResponse.MenuItemItem>> getPopularItems(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getPopularItems(limit).stream()
                .map(PublicMenuResponse::fromMenuItemResponse)
                .toList());
    }

    @GetMapping("/recommendations/items/{itemId}")
    public ApiResponse<List<PublicMenuResponse.MenuItemItem>> getRecommendations(
            @PathVariable @NonNull Long itemId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.getRecommendations(itemId, limit).stream()
                .map(PublicMenuResponse::fromMenuItemResponse)
                .toList());
    }
}
