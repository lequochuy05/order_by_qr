package com.qros.modules.payment.dto.response;

import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.shared.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCreateResponse(
        Long transactionId,
        Long orderId,
        PaymentMethod paymentMethod,
        PaymentTransactionStatus status,
        String checkoutUrl,
        String qrCode,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        BigDecimal amount,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String voucherCode,
        String idempotencyKey,
        String externalReference) {}
