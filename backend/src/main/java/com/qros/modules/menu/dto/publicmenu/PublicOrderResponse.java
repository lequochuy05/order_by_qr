package com.qros.modules.menu.dto.publicmenu;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PublicOrderResponse(
        Long id,
        String status,
        BigDecimal finalAmount,
        PublicTable table,
        List<PublicOrderItemResponse> orderItems,
        LocalDateTime createdAt
) {
}