package com.qros.modules.payment.model.enums;

import java.util.List;

public enum PaymentTransactionStatus {
    CREATING,
    PENDING,
    PAID,
    CANCELLED,
    EXPIRED,
    FAILED;

    public static final List<PaymentTransactionStatus> IN_PROGRESS_STATUSES = List.of(CREATING, PENDING);
}
