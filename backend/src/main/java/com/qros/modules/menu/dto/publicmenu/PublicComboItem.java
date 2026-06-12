package com.qros.modules.menu.dto.publicmenu;

import java.math.BigDecimal;
import java.util.List;

public record PublicComboItem(
        Long id,
        String name,
        String description,
        String img,
        BigDecimal price,
        List<PublicComboLine> items,
        Boolean available,
        Integer displayOrder
) {
}