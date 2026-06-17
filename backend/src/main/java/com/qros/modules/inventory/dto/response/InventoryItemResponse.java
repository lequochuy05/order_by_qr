package com.qros.modules.inventory.dto.response;

import java.math.BigDecimal;

public record InventoryItemResponse(
        Long id,
        String name,
        String unit,
        BigDecimal quantityOnHand,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        BigDecimal lowStockThreshold,
        Boolean lowStock,
        Boolean active) {}
