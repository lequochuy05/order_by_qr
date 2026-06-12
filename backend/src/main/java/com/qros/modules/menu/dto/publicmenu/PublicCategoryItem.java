package com.qros.modules.menu.dto.publicmenu;

public record PublicCategoryItem(
        Long id,
        String name,
        String img,
        Integer displayOrder
) {
}