package com.qros.modules.publicapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.inventory.service.InventoryAvailabilityService;
import com.qros.modules.menu.dto.publicmenu.PublicCatalogResponse;
import com.qros.modules.menu.dto.publicmenu.PublicCategoryItem;
import com.qros.modules.menu.dto.publicmenu.PublicComboItem;
import com.qros.modules.menu.dto.publicmenu.PublicMenuItem;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.service.CategoryService;
import com.qros.modules.menu.service.ComboService;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.modules.promotion.service.PromotionService;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.dto.response.PublicTableResponse;
import com.qros.modules.table.service.DiningTableService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicCatalogServiceTest {

    private CategoryService categoryService;
    private MenuItemService menuItemService;
    private ComboService comboService;
    private DiningTableService tableService;
    private InventoryAvailabilityService inventoryAvailabilityService;
    private PublicMenuMapper publicMenuMapper;
    private SystemSettingsService settingsService;
    private PromotionService promotionService;
    private PublicCatalogService publicCatalogService;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);
        menuItemService = mock(MenuItemService.class);
        comboService = mock(ComboService.class);
        tableService = mock(DiningTableService.class);
        inventoryAvailabilityService = mock(InventoryAvailabilityService.class);
        publicMenuMapper = new PublicMenuMapper();
        settingsService = mock(SystemSettingsService.class);
        promotionService = mock(PromotionService.class);

        publicCatalogService = new PublicCatalogService(
                categoryService,
                menuItemService,
                comboService,
                tableService,
                inventoryAvailabilityService,
                publicMenuMapper,
                settingsService,
                promotionService);
    }

    @Test
    void getCatalogFiltersUnavailableItemsAndHidesCombosWhenDisabled() {
        SystemSettings settings = SystemSettings.builder()
                .showUnavailableItems(false)
                .showCombos(false)
                .build();
        PublicCategoryItem cat = new PublicCategoryItem(1L, "Main", null, 1);
        PublicMenuItem availableItem =
                new PublicMenuItem(10L, "Pho", null, null, BigDecimal.valueOf(50000), null, List.of(), true, 1);
        PublicMenuItem outOfStockItem =
                new PublicMenuItem(11L, "Bun Bo", null, null, BigDecimal.valueOf(55000), null, List.of(), true, 2);

        when(settingsService.getSettingsEntity()).thenReturn(settings);
        when(categoryService.getPublicActive()).thenReturn(List.of(cat));
        when(menuItemService.getPublicMenuItems(false)).thenReturn(List.of(availableItem, outOfStockItem));
        when(inventoryAvailabilityService.getMenuItemAvailability(List.of(10L, 11L)))
                .thenReturn(Map.of(10L, true, 11L, false));

        PublicCatalogResponse catalog = publicCatalogService.getCatalog();

        assertThat(catalog.categories()).containsExactly(cat);
        assertThat(catalog.menuItems()).extracting(PublicMenuItem::id).containsExactly(10L);
        assertThat(catalog.combos()).isEmpty();
    }

    @Test
    void getCombosAppliesInventoryAvailability() {
        SystemSettings settings = SystemSettings.builder()
                .showUnavailableItems(false)
                .showCombos(true)
                .build();
        PublicComboItem combo =
                new PublicComboItem(20L, "Lunch Set", null, null, BigDecimal.valueOf(99000), List.of(), true, 1);

        when(settingsService.getSettingsEntity()).thenReturn(settings);
        when(comboService.getPublicActive(false)).thenReturn(List.of(combo));
        when(inventoryAvailabilityService.getComboAvailabilityByIds(List.of(20L)))
                .thenReturn(Map.of(20L, false));

        List<PublicComboItem> combos = publicCatalogService.getCombos();

        assertThat(combos).isEmpty();
    }

    @Test
    void getTableByCodeDelegatesToDiningTableService() {
        PublicTableResponse expected = new PublicTableResponse(1L, "T01");
        when(tableService.getPublicByCode("TABLE-1")).thenReturn(expected);

        PublicTableResponse actual = publicCatalogService.getTableByCode("TABLE-1");

        assertThat(actual).isEqualTo(expected);
        verify(tableService).getPublicByCode("TABLE-1");
    }
}
