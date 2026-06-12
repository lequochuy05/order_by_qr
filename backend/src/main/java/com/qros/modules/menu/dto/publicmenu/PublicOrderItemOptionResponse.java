package com.qros.modules.menu.dto.publicmenu;

import java.math.BigDecimal;

public record PublicOrderItemOptionResponse(
        String optionName,
        String optionValueName,
        BigDecimal extraPrice
) {
}