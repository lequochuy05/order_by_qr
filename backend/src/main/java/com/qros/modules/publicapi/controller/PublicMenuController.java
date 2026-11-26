package com.qros.modules.publicapi.controller;

import com.qros.modules.menu.dto.publicmenu.PublicCatalogResponse;
import com.qros.modules.menu.dto.publicmenu.PublicCategoryItem;
import com.qros.modules.menu.dto.publicmenu.PublicComboItem;
import com.qros.modules.menu.dto.publicmenu.PublicMenuItem;
import com.qros.modules.promotion.dto.response.PromotionResponse;
import com.qros.modules.publicapi.service.PublicCatalogService;
import com.qros.modules.table.dto.response.PublicTableResponse;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiRoutes.PUBLIC)
@RequiredArgsConstructor
public class PublicMenuController {

    private final PublicCatalogService publicCatalogService;

    @GetMapping("/promotions/active")
    public ApiResponse<List<PromotionResponse>> getActivePromotions() {
        return ApiResponse.success(publicCatalogService.getActivePromotions());
    }

    @GetMapping("/categories")
    public ApiResponse<List<PublicCategoryItem>> getCategories() {
        return ApiResponse.success(publicCatalogService.getCategories());
    }

    @GetMapping("/catalog")
    public ApiResponse<PublicCatalogResponse> getCatalog() {
        return ApiResponse.success(publicCatalogService.getCatalog());
    }

    @GetMapping("/menu-items")
    public ApiResponse<List<PublicMenuItem>> getMenuItems(@RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(publicCatalogService.getMenuItems(categoryId));
    }

    @GetMapping("/combos")
    public ApiResponse<List<PublicComboItem>> getCombos() {
        return ApiResponse.success(publicCatalogService.getCombos());
    }

    @GetMapping("/tables/by-code/{tableCode}")
    public ApiResponse<PublicTableResponse> getTableByCode(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(publicCatalogService.getTableByCode(tableCode));
    }
}
