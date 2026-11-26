package com.qros.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InventoryItemRequest(
        @NotBlank(message = "Inventory item name cannot be empty") @Size(max = 150, message = "Inventory item name cannot exceed 150 characters") String name,

        @NotBlank(message = "Inventory unit cannot be empty") @Size(max = 30, message = "Inventory unit cannot exceed 30 characters") String unit,

        @DecimalMin(value = "0.000", message = "Low stock threshold cannot be negative") BigDecimal lowStockThreshold,

        @DecimalMin(value = "0.00", message = "Unit cost cannot be negative") BigDecimal unitCost,

        Boolean active,
        Long version) {

    public InventoryItemRequest(String name, String unit, BigDecimal lowStockThreshold, Boolean active, Long version) {
        this(name, unit, lowStockThreshold, null, active, version);
    }
}
