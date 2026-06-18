package com.qros.modules.menu.dto.publicmenu;

import com.qros.modules.menu.dto.summary.CategorySummary;
import java.math.BigDecimal;
import java.util.List;

public record PublicMenuItem(
        Long id,
        String name,
        String description,
        String img,
        BigDecimal price,
        CategorySummary category,
        List<PublicItemOption> itemOptions,
        Boolean available,
        Integer displayOrder) {}
