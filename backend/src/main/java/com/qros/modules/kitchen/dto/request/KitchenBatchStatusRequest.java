package com.qros.modules.kitchen.dto.request;

import com.qros.modules.order.model.enums.OrderItemStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record KitchenBatchStatusRequest(
        @NotEmpty(message = "Item IDs must not be empty") List<Long> itemIds,
        @NotNull(message = "Status is required") OrderItemStatus status) {}
