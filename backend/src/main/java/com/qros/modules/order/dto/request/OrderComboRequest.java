package com.qros.modules.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderComboRequest(
        @NotNull(message = "Combo ID cannot be empty") Long comboId,

        @NotNull(message = "Quantity cannot be empty") @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 1000, message = "Quantity cannot exceed 1000") Integer quantity,

        @Size(max = 255, message = "Notes must not exceed 255 characters") String notes) {}
