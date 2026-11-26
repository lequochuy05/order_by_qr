package com.qros.modules.order.model.enums;

import java.util.List;

public enum OrderStatus {
    PENDING,
    SERVING,
    AWAITING_PAYMENT,
    COMPLETED,
    CANCELLED;

    public static final List<OrderStatus> ACTIVE_STATUSES = List.of(PENDING, SERVING, AWAITING_PAYMENT);
    public static final List<String> ACTIVE_STATUS_NAMES =
            ACTIVE_STATUSES.stream().map(Enum::name).toList();
}
