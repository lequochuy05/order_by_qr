package com.qros.modules.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderItemRequest(
        @NotNull(message = "Menu item ID cannot be empty") Long menuItemId,
        @NotNull(message = "Quantity cannot be empty") @Min(value = 1, message = "Quantity must be at least 1")
                Integer quantity,
        String notes,
        List<Long> selectedOptionValueIds) {}
