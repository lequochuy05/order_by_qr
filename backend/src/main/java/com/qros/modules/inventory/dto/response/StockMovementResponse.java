package com.qros.modules.inventory.dto.response;

import com.qros.modules.inventory.model.enums.StockMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long inventoryItemId,
        String inventoryItemName,
        String unit,
        Long orderItemId,
        StockMovementType type,
        BigDecimal quantity,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        String note,
        LocalDateTime createdAt) {
}