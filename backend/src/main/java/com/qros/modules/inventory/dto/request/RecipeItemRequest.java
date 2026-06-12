package com.qros.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecipeItemRequest(
        @NotNull(message = "Inventory item id cannot be empty")
        Long inventoryItemId,

        @NotNull(message = "Quantity required cannot be empty")
        @DecimalMin(value = "0.001", message = "Quantity required must be greater than 0")
        BigDecimal quantityRequired
) {
}