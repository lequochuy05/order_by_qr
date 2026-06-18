package com.qros.modules.publicapi.controller;

import com.qros.modules.inventory.service.InventoryAvailabilityService;
import com.qros.modules.menu.dto.publicmenu.PublicCatalogResponse;
import com.qros.modules.menu.dto.publicmenu.PublicCategoryItem;
import com.qros.modules.menu.dto.publicmenu.PublicComboItem;
import com.qros.modules.menu.dto.publicmenu.PublicMenuItem;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.service.CategoryService;
import com.qros.modules.menu.service.ComboService;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.modules.table.dto.response.PublicTable;
import com.qros.modules.table.service.DiningTableService;
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

    private final CategoryService categoryService;
    private final MenuItemService menuItemService;
    private final ComboService comboService;
    private final DiningTableService tableService;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final PublicMenuMapper publicMenuMapper;

    @GetMapping("/categories")
    public ApiResponse<List<PublicCategoryItem>> getCategories() {
        return ApiResponse.success(categoryService.getPublicActive());
    }

    @GetMapping("/catalog")
    public ApiResponse<PublicCatalogResponse> getCatalog() {
        return ApiResponse.success(new PublicCatalogResponse(
                categoryService.getPublicActive(),
                withMenuAvailability(menuItemService.getPublicMenuItems()),
                getAvailableCombos()));
    }

    @GetMapping("/menu-items")
    public ApiResponse<List<PublicMenuItem>> getMenuItems(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return ApiResponse.success(withMenuAvailability(menuItemService.getPublicItemsByCategory(categoryId)));
        }

        return ApiResponse.success(withMenuAvailability(menuItemService.getPublicMenuItems()));
    }

    @GetMapping("/combos")
    public ApiResponse<List<PublicComboItem>> getCombos() {
        return ApiResponse.success(getAvailableCombos());
    }

    @GetMapping("/tables/by-code/{tableCode}")
    public ApiResponse<PublicTable> getTableByCode(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableService.getPublicByCode(tableCode));
    }

    private List<PublicComboItem> getAvailableCombos() {
        var combos = comboService.getPublicActive();
        var comboIds = combos.stream().map(PublicComboItem::id).toList();
        var availability = inventoryAvailabilityService.getComboAvailabilityByIds(comboIds);

        return combos.stream()
                .map(combo -> publicMenuMapper.withAvailability(combo, availability.getOrDefault(combo.id(), true)))
                .toList();
    }

    private List<PublicMenuItem> withMenuAvailability(List<PublicMenuItem> items) {
        var itemIds = items.stream().map(PublicMenuItem::id).toList();

        var availability = inventoryAvailabilityService.getMenuItemAvailability(itemIds);

        return items.stream()
                .map(item -> publicMenuMapper.withAvailability(item, availability.getOrDefault(item.id(), true)))
                .toList();
    }
}
