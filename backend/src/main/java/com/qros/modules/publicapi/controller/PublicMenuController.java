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
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
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
    private final SystemSettingsService settingsService;

    @GetMapping("/categories")
    public ApiResponse<List<PublicCategoryItem>> getCategories() {
        return ApiResponse.success(categoryService.getPublicActive());
    }

    @GetMapping("/catalog")
    public ApiResponse<PublicCatalogResponse> getCatalog() {
        SystemSettings settings = settingsService.getSettingsEntity();
        boolean includeUnavailable = Boolean.TRUE.equals(settings.getShowUnavailableItems());
        return ApiResponse.success(new PublicCatalogResponse(
                categoryService.getPublicActive(),
                withMenuAvailability(menuItemService.getPublicMenuItems(includeUnavailable), includeUnavailable),
                getConfiguredCombos(settings)));
    }

    @GetMapping("/menu-items")
    public ApiResponse<List<PublicMenuItem>> getMenuItems(@RequestParam(required = false) Long categoryId) {
        boolean includeUnavailable =
                Boolean.TRUE.equals(settingsService.getSettingsEntity().getShowUnavailableItems());
        if (categoryId != null) {
            return ApiResponse.success(withMenuAvailability(
                    menuItemService.getPublicItemsByCategory(categoryId, includeUnavailable), includeUnavailable));
        }

        return ApiResponse.success(
                withMenuAvailability(menuItemService.getPublicMenuItems(includeUnavailable), includeUnavailable));
    }

    @GetMapping("/combos")
    public ApiResponse<List<PublicComboItem>> getCombos() {
        return ApiResponse.success(getConfiguredCombos(settingsService.getSettingsEntity()));
    }

    @GetMapping("/tables/by-code/{tableCode}")
    public ApiResponse<PublicTable> getTableByCode(@PathVariable @NonNull String tableCode) {
        return ApiResponse.success(tableService.getPublicByCode(tableCode));
    }

    private List<PublicComboItem> getConfiguredCombos(SystemSettings settings) {
        if (!Boolean.TRUE.equals(settings.getShowCombos())) {
            return List.of();
        }

        var combos = comboService.getPublicActive(Boolean.TRUE.equals(settings.getShowUnavailableItems()));
        var comboIds = combos.stream().map(PublicComboItem::id).toList();
        var availability = inventoryAvailabilityService.getComboAvailabilityByIds(comboIds);

        List<PublicComboItem> configuredCombos = combos.stream()
                .map(combo -> publicMenuMapper.withAvailability(
                        combo, Boolean.TRUE.equals(combo.available()) && availability.getOrDefault(combo.id(), true)))
                .toList();

        if (Boolean.TRUE.equals(settings.getShowUnavailableItems())) {
            return configuredCombos;
        }

        return configuredCombos.stream()
                .filter(combo -> Boolean.TRUE.equals(combo.available()))
                .toList();
    }

    private List<PublicMenuItem> withMenuAvailability(List<PublicMenuItem> items, boolean includeUnavailable) {
        var itemIds = items.stream().map(PublicMenuItem::id).toList();

        var availability = inventoryAvailabilityService.getMenuItemAvailability(itemIds);

        List<PublicMenuItem> configuredItems = items.stream()
                .map(item -> publicMenuMapper.withAvailability(
                        item, Boolean.TRUE.equals(item.available()) && availability.getOrDefault(item.id(), true)))
                .toList();

        if (includeUnavailable) {
            return configuredItems;
        }

        return configuredItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.available()))
                .toList();
    }
}
