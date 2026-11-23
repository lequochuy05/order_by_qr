package com.qros.modules.inventory.dto.response;

import java.math.BigDecimal;

public record RecipeItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Long inventoryItemId,
        String inventoryItemName,
        String unit,
        BigDecimal quantityRequired,
        BigDecimal availableQuantity,
        Boolean lowStock,
        Boolean outOfStock) {

    public RecipeItemResponse(
            Long id,
            Long menuItemId,
            String menuItemName,
            Long inventoryItemId,
            String inventoryItemName,
            String unit,
            BigDecimal quantityRequired) {
        this(
                id,
                menuItemId,
                menuItemName,
                inventoryItemId,
                inventoryItemName,
                unit,
                quantityRequired,
                null,
                null,
                null);
    }
}
