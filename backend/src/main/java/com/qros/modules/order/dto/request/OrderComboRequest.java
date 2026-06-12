package com.qros.modules.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderComboRequest(
        @NotNull(message = "Combo ID cannot be empty")
        Long comboId,

        @NotNull(message = "Quantity cannot be empty")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        String notes) {
}
