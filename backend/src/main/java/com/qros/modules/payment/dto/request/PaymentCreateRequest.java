package com.qros.modules.payment.dto.request;

import com.qros.shared.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaymentCreateRequest(
        @NotNull(message = "Order ID is required") Long orderId,
        @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
        @Size(max = 50, message = "Voucher code must not exceed 50 characters") String voucherCode,
        @Size(max = 100, message = "Idempotency key must not exceed 100 characters") String idempotencyKey,
        /** Amount of cash handed by the customer. Used for change calculation (cash payments only). */
        BigDecimal cashReceived,
        /** Pre-calculated change to give back. Stored for shift reconciliation (cash payments only). */
        BigDecimal changeAmount,
        /** When true, allows settlement even if kitchen items are still PENDING/COOKING. */
        Boolean confirmUnfinishedItems) {

    public PaymentCreateRequest(Long orderId, PaymentMethod paymentMethod, String voucherCode, String idempotencyKey) {
        this(orderId, paymentMethod, voucherCode, idempotencyKey, null, null, null);
    }
}
