package com.qros.modules.inventory.dto.internal;

import java.math.BigDecimal;

public record InventoryRequirement(
        Long inventoryItemId,
        String inventoryItemName,
        String unit,
        BigDecimal requiredQuantity,
        BigDecimal availableQuantity,
        boolean sufficient) {}
