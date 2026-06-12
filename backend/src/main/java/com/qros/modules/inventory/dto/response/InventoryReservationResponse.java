package com.qros.modules.inventory.dto.response;

import com.qros.modules.inventory.model.enums.InventoryReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryReservationResponse(
        Long id,
        Long orderItemId,
        Long inventoryItemId,
        String inventoryItemName,
        String unit,
        BigDecimal reservedQuantity,
        InventoryReservationStatus status,
        LocalDateTime reservedAt,
        LocalDateTime releasedAt,
        LocalDateTime consumedAt) {
}