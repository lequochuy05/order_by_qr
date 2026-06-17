package com.qros.modules.payment.dto.request;

import com.qros.shared.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentCreateRequest(
        @NotNull(message = "Order ID is required") Long orderId,
        @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
        @Size(max = 50, message = "Voucher code must not exceed 50 characters") String voucherCode,
        @Size(max = 100, message = "Idempotency key must not exceed 100 characters") String idempotencyKey) {}
