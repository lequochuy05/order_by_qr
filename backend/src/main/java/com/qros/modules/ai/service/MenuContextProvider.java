package com.qros.modules.ai.service;

import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.cache.CacheNames;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuContextProvider {

    private final MenuItemRepository menuItemRepository;
    private final ComboRepository comboRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = CacheNames.AI_MENU_CONTEXT, key = "'current'")
    public String buildCurrentMenuContext() {
        List<MenuItem> activeMenu = menuItemRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc();
        List<Combo> activeCombos = comboRepository.findAllActiveWithItems();
        List<Category> activeCategories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();

        return buildMenuContext(activeMenu, activeCombos, activeCategories);
    }

    private String buildMenuContext(List<MenuItem> items, List<Combo> combos, List<Category> categories) {
        if (items.isEmpty() && combos.isEmpty() && categories.isEmpty()) {
            return "Hiện tại thực đơn đang trống.";
        }

        Map<String, List<MenuItem>> grouped = items.stream()
                .filter(item -> item.getCategory() != null)
                .filter(item -> Boolean.TRUE.equals(item.getCategory().getActive()))
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
            builder.append("\nMÓN LẺ THEO DANH MỤC:\n  Hiện chưa có món lẻ đang bán.\n");
            return;
        }

        builder.append("\nMÓN LẺ THEO DANH MỤC:\n");

        groupedItems.forEach((category, menuItems) -> {
            builder.append("Danh mục ").append(category).append(":\n");

            menuItems.forEach(item -> {
                builder.append("  - ").append(item.getName()).append(" | Giá: ").append(formatPrice(item.getPrice()));
                if (item.getDescription() != null && !item.getDescription().isBlank()) {
                    builder.append(" | Mô tả: ").append(item.getDescription());
                }
                builder.append("\n");
            });
        });
    }

    private void appendComboContext(StringBuilder builder, List<Combo> combos) {
        List<Combo> activeCombos = combos.stream()
                .filter(combo -> Boolean.TRUE.equals(combo.getActive()))
                .toList();

        if (activeCombos.isEmpty()) {
            builder.append("\nCOMBO ĐANG BÁN:\n  Hiện chưa có combo đang bán.\n");
            return;
        }

        builder.append("\nCOMBO ĐANG BÁN:\n");

        activeCombos.forEach(combo -> {
            builder.append("Combo ")
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
                .filter(comboItem -> comboItem.getMenuItem() != null)
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
}
