package com.qros.modules.inventory.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record StockAdjustmentRequest(
        @NotNull(message = "Quantity delta cannot be empty") BigDecimal quantityDelta,
        @Size(max = 500, message = "Note cannot exceed 500 characters") String note) {

    @AssertTrue(message = "Quantity delta cannot be zero")
    public boolean isQuantityDeltaNotZero() {
        return quantityDelta != null && quantityDelta.compareTo(BigDecimal.ZERO) != 0;
    }
}
