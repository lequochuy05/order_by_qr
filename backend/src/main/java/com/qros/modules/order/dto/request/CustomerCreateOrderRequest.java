package com.qros.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CustomerCreateOrderRequest(
        @NotBlank(message = "Table code is required") String tableCode,
        @NotBlank(message = "Session token is required") String sessionToken,
        @NotBlank(message = "Client request ID is required")
                @Size(max = 100, message = "Client request ID must not exceed 100 characters")
                String clientRequestId,
        @Valid List<OrderComboRequest> combos,
        @Valid List<OrderItemRequest> items) {}
