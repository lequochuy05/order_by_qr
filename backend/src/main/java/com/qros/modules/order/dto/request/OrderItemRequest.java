package com.qros.modules.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderItemRequest(
        @NotNull(message = "Menu item ID cannot be empty") Long menuItemId,

        @NotNull(message = "Quantity cannot be empty") @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 1000, message = "Quantity cannot exceed 1000") Integer quantity,

        @Size(max = 255, message = "Notes must not exceed 255 characters") String notes,

        List<Long> selectedOptionValueIds) {}
