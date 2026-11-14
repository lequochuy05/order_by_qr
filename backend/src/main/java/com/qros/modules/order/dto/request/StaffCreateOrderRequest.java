package com.qros.modules.order.dto.request;

import com.qros.modules.order.model.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StaffCreateOrderRequest(
        Long tableId,
        @Valid List<OrderComboRequest> combos,
        @Valid List<OrderItemRequest> items,
        @Size(max = 50, message = "Voucher code must not exceed 50 characters") String voucherCode,
        OrderType orderType,
        @Size(max = 100, message = "Client request ID must not exceed 100 characters") String clientRequestId) {

    public StaffCreateOrderRequest(
            Long tableId,
            List<OrderComboRequest> combos,
            List<OrderItemRequest> items,
            String voucherCode,
            OrderType orderType) {
        this(tableId, combos, items, voucherCode, orderType, null);
    }
}
