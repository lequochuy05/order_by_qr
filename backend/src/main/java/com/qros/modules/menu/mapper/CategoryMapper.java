package com.qros.modules.menu.mapper;

import com.qros.modules.menu.dto.response.CategoryResponse;
import com.qros.modules.menu.dto.response.MenuItemResponse;
import com.qros.modules.menu.dto.summary.CategorySummary;
import com.qros.modules.menu.model.Category;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    private final MenuItemMapper menuItemMapper;

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }

        List<MenuItemResponse> menuItems = category.getMenuItems() == null
                ? Collections.emptyList()
                : category.getMenuItems().stream()
                        .filter(item -> !item.isDeleted())
                        .map(menuItemMapper::toResponse)
                        .toList();

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getImg(),
                category.getActive(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getVersion(),
                menuItems);
    }

    public CategoryResponse toSummaryResponse(Category category) {
        if (category == null) {
            return null;
        }

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getImg(),
                category.getActive(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getVersion(),
                Collections.emptyList());
    }

    public CategorySummary toSummary(Category category) {
        if (category == null) {
            return null;
        }

        return new CategorySummary(category.getId(), category.getName());
    }
}
