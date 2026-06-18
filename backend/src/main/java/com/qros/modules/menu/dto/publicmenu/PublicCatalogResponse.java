package com.qros.modules.menu.dto.publicmenu;

import java.util.List;

public record PublicCatalogResponse(
        List<PublicCategoryItem> categories, List<PublicMenuItem> menuItems, List<PublicComboItem> combos) {}
