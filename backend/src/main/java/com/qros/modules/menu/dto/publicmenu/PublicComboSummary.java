package com.qros.modules.menu.dto.publicmenu;

import java.math.BigDecimal;

public record PublicComboSummary(
        Long id,
        String name,
        BigDecimal price
) {
}