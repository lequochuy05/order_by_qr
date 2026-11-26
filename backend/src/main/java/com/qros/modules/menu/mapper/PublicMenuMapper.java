package com.qros.modules.menu.mapper;

import com.qros.modules.menu.dto.publicmenu.*;
import com.qros.modules.menu.dto.summary.CategorySummary;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublicMenuMapper {

    private static final Comparator<ItemOption> ITEM_OPTION_COMPARATOR = MenuOptionComparators.ITEM_OPTION_COMPARATOR;
    private static final Comparator<ItemOptionValue> ITEM_OPTION_VALUE_COMPARATOR =
            MenuOptionComparators.ITEM_OPTION_VALUE_COMPARATOR;

    public PublicCategoryItem toCategoryItem(Category category) {
        if (category == null) {
            return null;
        }

        return new PublicCategoryItem(
                category.getId(), category.getName(), category.getImg(), category.getDisplayOrder());
    }

    public PublicMenuItem toMenuItem(MenuItem item) {
        if (item == null) {
            return null;
        }

        List<PublicItemOption> options = item.getItemOptions() == null
                ? Collections.emptyList()
                : item.getItemOptions().stream()
                        .filter(option -> !option.isDeleted())
                        .sorted(ITEM_OPTION_COMPARATOR)
                        .map(this::toItemOption)
                        .toList();

        return new PublicMenuItem(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getImg(),
                item.getPrice(),
                item.getCategory() == null
                        ? null
                        : new CategorySummary(
                                item.getCategory().getId(), item.getCategory().getName()),
                options,
                item.getAvailable(),
                item.getDisplayOrder());
    }

    public PublicItemOption toItemOption(ItemOption option) {
        if (option == null) {
            return null;
        }

        List<PublicItemOptionValue> values = option.getOptionValues() == null
                ? Collections.emptyList()
                : option.getOptionValues().stream()
                        .filter(value -> !value.isDeleted())
                        .sorted(ITEM_OPTION_VALUE_COMPARATOR)
                        .map(this::toItemOptionValue)
                        .toList();

        return new PublicItemOption(
                option.getId(),
                option.getName(),
                option.getRequired(),
                option.getMaxSelection(),
                option.getDisplayOrder(),
                values);
    }

    public PublicItemOptionValue toItemOptionValue(ItemOptionValue value) {
        if (value == null) {
            return null;
        }

        return new PublicItemOptionValue(
                value.getId(), value.getName(), value.getExtraPrice(), value.getDisplayOrder());
    }

    public PublicComboItem toComboItem(Combo combo) {
        if (combo == null) {
            return null;
        }

        List<PublicComboLine> lines = combo.getItems() == null
                ? Collections.emptyList()
                : combo.getItems().stream()
                        .filter(item -> !item.isDeleted())
                        .filter(item -> item.getMenuItem() != null)
                        .sorted(Comparator.comparing(item -> item.getMenuItem().getName() == null
                                ? ""
                                : item.getMenuItem().getName()))
                        .map(item -> new PublicComboLine(
                                item.getMenuItem().getName(), item.getMenuItem().getImg(), item.getQuantity()))
                        .toList();

        boolean comboAvailable = Boolean.TRUE.equals(combo.getAvailable())
                && (combo.getItems() == null
                        || combo.getItems().isEmpty()
                        || combo.getItems().stream()
                                .filter(item -> !item.isDeleted())
                                .allMatch(item -> item.getMenuItem() != null
                                        && !item.getMenuItem().isDeleted()
                                        && Boolean.TRUE.equals(
                                                item.getMenuItem().getActive())
                                        && Boolean.TRUE.equals(
                                                item.getMenuItem().getAvailable())));

        return new PublicComboItem(
                combo.getId(),
                combo.getName(),
                combo.getDescription(),
                null,
                combo.getPrice(),
                lines,
                comboAvailable,
                combo.getDisplayOrder());
    }

    public PublicMenuItem withAvailability(PublicMenuItem item, boolean available) {
        if (item == null) {
            return null;
        }

        return new PublicMenuItem(
                item.id(),
                item.name(),
                item.description(),
                item.img(),
                item.price(),
                item.category(),
                item.itemOptions(),
                available,
                item.displayOrder());
    }

    public PublicComboItem withAvailability(PublicComboItem combo, boolean available) {
        if (combo == null) {
            return null;
        }

        return new PublicComboItem(
                combo.id(),
                combo.name(),
                combo.description(),
                combo.img(),
                combo.price(),
                combo.items(),
                available,
                combo.displayOrder());
    }
}
