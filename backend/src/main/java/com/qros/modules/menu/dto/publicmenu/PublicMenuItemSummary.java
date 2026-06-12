package com.qros.modules.menu.dto.publicmenu;

public record PublicMenuItemSummary(
        Long id,
        String name,
        PublicCategoryName category
) {
}