package com.qros.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CustomerCreateOrderRequest(
        @NotBlank(message = "Table code is required") String tableCode,

        @Valid List<OrderComboRequest> combos,

        @Valid List<OrderItemRequest> items) {
}