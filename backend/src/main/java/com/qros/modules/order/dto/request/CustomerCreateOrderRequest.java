package com.qros.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CustomerCreateOrderRequest(
        @NotBlank(message = "Table code is required") String tableCode,

        @NotBlank(message = "Session token is required") String sessionToken,

        @Valid List<OrderComboRequest> combos,

        @Valid List<OrderItemRequest> items) {
}
