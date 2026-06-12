package com.qros.modules.kitchen.dto.request;

import com.qros.modules.order.model.enums.OrderItemStatus;
import jakarta.validation.constraints.NotNull;

public record KitchenItemStatusRequest(
        @NotNull(message = "Status is required") OrderItemStatus status) {
}
