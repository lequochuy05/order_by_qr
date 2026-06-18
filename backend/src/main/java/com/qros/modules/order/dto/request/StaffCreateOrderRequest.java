package com.qros.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StaffCreateOrderRequest(
        @NotNull(message = "Table ID is required") Long tableId,
        @Valid List<OrderComboRequest> combos,
        @Valid List<OrderItemRequest> items,
        @Size(max = 50, message = "Voucher code must not exceed 50 characters") String voucherCode) {}
