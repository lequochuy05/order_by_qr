package com.qros.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record BatchStockInRequest(
        @Size(max = 150, message = "Supplier name cannot exceed 150 characters") String supplierName,

        @Size(max = 100, message = "Invoice reference cannot exceed 100 characters") String invoiceRef,

        @Size(max = 300, message = "Note cannot exceed 300 characters") String note,

        @NotEmpty(message = "At least one stock-in item is required") @Size(max = 50, message = "Cannot import more than 50 lines at once") List<@Valid BatchStockInLine> items) {

    public record BatchStockInLine(
            @NotNull(message = "Inventory item id is required") @Min(value = 1, message = "Inventory item id must be positive") Long inventoryItemId,

            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than 0") BigDecimal quantity,

            @DecimalMin(value = "0.00", message = "Unit cost cannot be negative") BigDecimal unitCost,

            Boolean updateUnitCost,

            @Size(max = 300, message = "Line note cannot exceed 300 characters") String note) {}
}
