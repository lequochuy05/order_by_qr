package com.qros.modules.ai.service;

import com.qros.modules.inventory.service.InventoryAvailabilityService;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.shared.cache.CacheNames;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuContextProvider {

    private final MenuCatalogLookupService menuCatalogLookupService;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final SystemSettingsService systemSettingsService;

    public MenuContextProvider(
            MenuCatalogLookupService menuCatalogLookupService,
            InventoryAvailabilityService inventoryAvailabilityService) {
        this(menuCatalogLookupService, inventoryAvailabilityService, null);
    }

    @Autowired
    public MenuContextProvider(
            MenuCatalogLookupService menuCatalogLookupService,
            InventoryAvailabilityService inventoryAvailabilityService,
            SystemSettingsService systemSettingsService) {
        this.menuCatalogLookupService = menuCatalogLookupService;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.systemSettingsService = systemSettingsService;
    }

    @Cacheable(value = CacheNames.AI_MENU_CONTEXT, key = "'current'")
    @Transactional(readOnly = true)
    public String buildCurrentMenuContext() {
        MenuCatalogSnapshot snapshot = loadAvailableCatalog();
        List<Category> activeCategories = menuCatalogLookupService.findActiveCategories();

        return buildMenuContext(
                snapshot.menuItemsById().values().stream().toList(),
                snapshot.combosById().values().stream().toList(),
                activeCategories);
    }

    @Transactional(readOnly = true)
    public MenuCatalogSnapshot loadAvailableCatalog() {
        List<MenuItem> candidateItems = menuCatalogLookupService.findAllPublicAvailableMenuItems();
        List<Long> candidateItemIds = candidateItems.stream()
                .map(MenuItem::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Boolean> itemStockAvailability = candidateItemIds.isEmpty()
                ? Map.of()
                : inventoryAvailabilityService.getMenuItemAvailability(candidateItemIds);

        Map<Long, MenuItem> availableItemsById = candidateItems.stream()
                .filter(this::isMenuItemActiveAndAvailable)
                .filter(item -> itemStockAvailability.getOrDefault(item.getId(), true))
                .collect(Collectors.toMap(
                        MenuItem::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        boolean showCombos = systemSettingsService == null
                || !Boolean.FALSE.equals(
                        systemSettingsService.getSettingsEntity().getShowCombos());

        Map<Long, Combo> availableCombosById = Map.of();
        if (showCombos) {
            List<Combo> candidateCombos = menuCatalogLookupService.findAllActiveCombosWithItems();
            List<Long> candidateComboIds = candidateCombos.stream()
                    .map(Combo::getId)
                    .filter(Objects::nonNull)
                    .toList();
            Map<Long, Boolean> comboStockAvailability = candidateComboIds.isEmpty()
                    ? Map.of()
                    : inventoryAvailabilityService.getComboAvailabilityByIds(candidateComboIds);

            availableCombosById = candidateCombos.stream()
                    .filter(this::isComboActiveAndAvailable)
                    .filter(combo -> comboStockAvailability.getOrDefault(combo.getId(), true))
                    .collect(Collectors.toMap(
                            Combo::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        }

        return new MenuCatalogSnapshot(availableItemsById, availableCombosById);
    }

    private boolean isMenuItemActiveAndAvailable(MenuItem item) {
        return item != null
                && item.getId() != null
                && !item.isDeleted()
                && Boolean.TRUE.equals(item.getActive())
                && Boolean.TRUE.equals(item.getAvailable())
                && item.getCategory() != null
                && Boolean.TRUE.equals(item.getCategory().getActive());
    }

    private boolean isComboActiveAndAvailable(Combo combo) {
        if (combo == null
                || combo.getId() == null
                || combo.isDeleted()
                || !Boolean.TRUE.equals(combo.getActive())
                || !Boolean.TRUE.equals(combo.getAvailable())) {
            return false;
        }
        return inventoryAvailabilityService.areAllComboItemsOrderable(combo);
    }

    private String buildMenuContext(List<MenuItem> items, List<Combo> combos, List<Category> categories) {
        if (items.isEmpty() && combos.isEmpty() && categories.isEmpty()) {
            return "Hiện tại thực đơn đang trống.";
        }

        Map<String, List<MenuItem>> grouped = items.stream()
                .filter(this::isMenuItemActiveAndAvailable)
                .collect(Collectors.groupingBy(
                        item -> item.getCategory().getName(), LinkedHashMap::new, Collectors.toList()));

        StringBuilder builder = new StringBuilder();

        appendCategoryContext(builder, categories, grouped.keySet());
        appendMenuItemContext(builder, grouped);
        appendComboContext(builder, combos);

        return builder.toString();
    }

    private void appendCategoryContext(
            StringBuilder builder, List<Category> categories, Set<String> categoryNamesFromMenu) {
        Set<String> categoryNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        categories.stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .map(Category::getName)
                .filter(Objects::nonNull)
                .forEach(categoryNames::add);

        categoryNames.addAll(categoryNamesFromMenu);

        if (categoryNames.isEmpty()) {
            return;
        }

        builder.append("\nDANH MỤC ĐANG CÓ:\n");
        categoryNames.forEach(
                category -> builder.append("  - ").append(category).append("\n"));
    }

    private void appendMenuItemContext(StringBuilder builder, Map<String, List<MenuItem>> groupedItems) {
        if (groupedItems.isEmpty()) {
            builder.append("\nMÓN LẺ ĐANG CÒN HÀNG THEO DANH MỤC:\n  Hiện chưa có món lẻ còn hàng.\n");
            return;
        }

        builder.append("\nMÓN LẺ ĐANG CÒN HÀNG THEO DANH MỤC:\n");

        groupedItems.forEach((category, menuItems) -> {
            builder.append("Danh mục ").append(category).append(":\n");

            menuItems.forEach(item -> {
                builder.append("  - [ITEM:")
                        .append(item.getId())
                        .append("] ")
                        .append(item.getName())
                        .append(" | Giá: ")
                        .append(formatPrice(item.getPrice()));
                if (hasActiveOptions(item)) {
                    builder.append(" | Có tùy chọn");
                }
                if (item.getDescription() != null && !item.getDescription().isBlank()) {
                    builder.append(" | Mô tả: ").append(item.getDescription());
                }
                builder.append("\n");
            });
        });
    }

    private boolean hasActiveOptions(MenuItem item) {
        return item.getItemOptions() != null
                && item.getItemOptions().stream().anyMatch(option -> option != null && !option.isDeleted());
    }

    private void appendComboContext(StringBuilder builder, List<Combo> combos) {
        List<Combo> activeCombos = combos.stream()
                .filter(combo -> Boolean.TRUE.equals(combo.getActive()) && Boolean.TRUE.equals(combo.getAvailable()))
                .toList();

        if (activeCombos.isEmpty()) {
            builder.append("\nCOMBO ĐANG BÁN:\n  Hiện chưa có combo đang bán.\n");
            return;
        }

        builder.append("\nCOMBO ĐANG BÁN:\n");

        activeCombos.forEach(combo -> {
            builder.append("  - [COMBO:")
                    .append(combo.getId())
                    .append("] Combo ")
                    .append(combo.getName())
                    .append(" | Giá combo: ")
                    .append(formatPrice(combo.getPrice()));

            String comboItems = formatComboItems(combo.getItems());

            if (!comboItems.isBlank()) {
                builder.append(" | Gồm: ").append(comboItems);
            }

            builder.append("\n");
        });
    }

    private String formatComboItems(Set<ComboItem> comboItems) {
        if (comboItems == null || comboItems.isEmpty()) {
            return "";
        }

        return comboItems.stream()
                .filter(comboItem -> !comboItem.isDeleted() && comboItem.getMenuItem() != null)
                .map(comboItem -> {
                    Integer quantity = comboItem.getQuantity() != null ? comboItem.getQuantity() : 1;
                    return quantity + " x " + comboItem.getMenuItem().getName();
                })
                .collect(Collectors.joining(", "));
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "Liên hệ";
        }

        return "%,.0fđ".formatted(price).replace(",", ".");
    }

    public record MenuCatalogSnapshot(Map<Long, MenuItem> menuItemsById, Map<Long, Combo> combosById) {
        public MenuCatalogSnapshot {
            menuItemsById = menuItemsById == null ? Map.of() : Map.copyOf(menuItemsById);
            combosById = combosById == null ? Map.of() : Map.copyOf(combosById);
        }
    }
}
