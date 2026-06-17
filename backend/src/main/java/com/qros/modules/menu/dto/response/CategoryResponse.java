package com.qros.modules.menu.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CategoryResponse - Data transfer object for food and beverage categories.
 */
public record CategoryResponse(
        Long id,
        String name,
        String img,
        Boolean active,
        String description,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MenuItemResponse> menuItems) {}
