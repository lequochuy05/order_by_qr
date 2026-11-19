package com.qros.modules.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record OrderItemUpdateRequest(
        @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 1000, message = "Quantity cannot exceed 1000") Integer quantity,

        String notes) {}
