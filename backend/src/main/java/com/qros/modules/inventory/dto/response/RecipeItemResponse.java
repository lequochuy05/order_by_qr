package com.qros.modules.inventory.dto.response;

import java.math.BigDecimal;

public record RecipeItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Long inventoryItemId,
        String inventoryItemName,
        String unit,
        BigDecimal quantityRequired) {}
