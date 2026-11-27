package com.qros.modules.publicapi.service;

import com.qros.modules.inventory.service.InventoryAvailabilityService;
import com.qros.modules.menu.dto.publicmenu.PublicCatalogResponse;
import com.qros.modules.menu.dto.publicmenu.PublicCategoryItem;
import com.qros.modules.menu.dto.publicmenu.PublicComboItem;
import com.qros.modules.menu.dto.publicmenu.PublicMenuItem;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.service.CategoryService;
import com.qros.modules.menu.service.ComboService;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.modules.promotion.dto.response.PromotionResponse;
import com.qros.modules.promotion.service.PromotionService;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.dto.response.PublicTableResponse;
import com.qros.modules.table.service.DiningTableService;
import com.qros.shared.cache.CacheNames;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregation facade serving public customer catalog, promotions, and table lookup endpoints.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCatalogService {

    private final CategoryService categoryService;
    private final MenuItemService menuItemService;
    private final ComboService comboService;
    private final DiningTableService tableService;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final PublicMenuMapper publicMenuMapper;
    private final SystemSettingsService settingsService;
    private final PromotionService promotionService;

    public List<PromotionResponse> getActivePromotions() {
        return promotionService.findActivePromotionResponses();
    }

    public List<PublicCategoryItem> getCategories() {
        return categoryService.getPublicActive();
    }

    @Cacheable(value = CacheNames.PUBLIC_MENU, key = "'catalog'")
    public PublicCatalogResponse getCatalog() {
        SystemSettings settings = settingsService.getSettingsEntity();
        boolean includeUnavailable = Boolean.TRUE.equals(settings.getShowUnavailableItems());
        return new PublicCatalogResponse(
                categoryService.getPublicActive(),
                withMenuAvailability(menuItemService.getPublicMenuItems(includeUnavailable), includeUnavailable),
                getConfiguredCombos(settings));
    }

    public List<PublicMenuItem> getMenuItems(Long categoryId) {
        boolean includeUnavailable =
                Boolean.TRUE.equals(settingsService.getSettingsEntity().getShowUnavailableItems());
        if (categoryId != null) {
            return withMenuAvailability(
                    menuItemService.getPublicItemsByCategory(categoryId, includeUnavailable), includeUnavailable);
        }

        return withMenuAvailability(menuItemService.getPublicMenuItems(includeUnavailable), includeUnavailable);
    }

    public List<PublicComboItem> getCombos() {
        return getConfiguredCombos(settingsService.getSettingsEntity());
    }

    public PublicTableResponse getTableByCode(@NonNull String tableCode) {
        return tableService.getPublicByCode(tableCode);
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
