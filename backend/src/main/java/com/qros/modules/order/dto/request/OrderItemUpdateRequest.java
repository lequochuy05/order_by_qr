package com.qros.modules.order.dto.request;

import jakarta.validation.constraints.Min;

public record OrderItemUpdateRequest(
    @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
    String notes) {
}
