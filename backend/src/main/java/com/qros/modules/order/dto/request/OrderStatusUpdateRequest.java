package com.qros.modules.order.dto.request;

import com.qros.modules.order.model.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
    @NotNull(message = "Status is required")
    OrderStatus status) {
}
