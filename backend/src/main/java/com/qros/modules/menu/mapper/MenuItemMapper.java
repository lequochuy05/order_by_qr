package com.qros.modules.menu.mapper;

import com.qros.modules.menu.dto.response.ItemOptionResponse;
import com.qros.modules.menu.dto.response.ItemOptionValueResponse;
import com.qros.modules.menu.dto.response.MenuItemResponse;
import com.qros.modules.menu.dto.summary.CategorySummary;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class MenuItemMapper {

    private static final Comparator<ItemOption> ITEM_OPTION_COMPARATOR = Comparator.comparing(
            (ItemOption option) -> option.getDisplayOrder() == null ? 0 : option.getDisplayOrder()).thenComparing(
                    (ItemOption option) -> option.getName() == null ? "" : option.getName());

    private static final Comparator<ItemOptionValue> ITEM_OPTION_VALUE_COMPARATOR = Comparator.comparing(
            (ItemOptionValue value) -> value.getDisplayOrder() == null ? 0 : value.getDisplayOrder()).thenComparing(
                    (ItemOptionValue value) -> value.getName() == null ? "" : value.getName());

    public MenuItemResponse toResponse(MenuItem item) {
        if (item == null) {
            return null;
        }

        List<ItemOptionResponse> itemOptions = item.getItemOptions() == null
                ? Collections.emptyList()
                : item.getItemOptions().stream()
                        .filter(option -> !option.isDeleted())
                        .sorted(ITEM_OPTION_COMPARATOR)
                        .map(this::toOptionResponse)
                        .toList();

        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getImg(),
                item.getPrice(),
                item.getActive(),
                item.getAvailable(),
                item.getDisplayOrder(),
                toCategorySummary(item.getCategory()),
                itemOptions,
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public MenuItemResponse toSummaryResponse(MenuItem item) {
        if (item == null) {
            return null;
        }

        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getImg(),
                item.getPrice(),
                item.getActive(),
                item.getAvailable(),
                item.getDisplayOrder(),
                toCategorySummary(item.getCategory()),
                Collections.emptyList(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public ItemOptionResponse toOptionResponse(ItemOption option) {
        if (option == null) {
            return null;
        }

        List<ItemOptionValueResponse> optionValues = option.getOptionValues() == null
                ? Collections.emptyList()
                : option.getOptionValues().stream()
                        .filter(value -> !value.isDeleted())
                        .sorted(ITEM_OPTION_VALUE_COMPARATOR)
                        .map(this::toOptionValueResponse)
                        .toList();

        return new ItemOptionResponse(
                option.getId(),
                option.getName(),
                option.getRequired(),
                option.getMaxSelection(),
                option.getDisplayOrder(),
                optionValues);
    }

    public ItemOptionValueResponse toOptionValueResponse(ItemOptionValue value) {
        if (value == null) {
            return null;
        }

        return new ItemOptionValueResponse(
                value.getId(),
                value.getName(),
                value.getExtraPrice(),
                value.getDisplayOrder());
    }

    private CategorySummary toCategorySummary(Category category) {
        if (category == null) {
            return null;
        }

        return new CategorySummary(
                category.getId(),
                category.getName());
    }
}
