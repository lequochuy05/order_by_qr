package com.qros.modules.menu.dto.publicmenu;

import java.math.BigDecimal;
import java.util.List;

public record PublicOrderItemResponse(
        Long id,
        PublicMenuItemSummary menuItem,
        PublicComboSummary combo,
        BigDecimal unitPrice,
        Integer quantity,
        String notes,
        String status,
        List<PublicOrderItemOptionResponse> options
) {
}