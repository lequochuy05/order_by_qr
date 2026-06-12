package com.qros.modules.payment.dto.response;

import com.qros.shared.enums.PaymentMethod;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        Long transactionId,
        Long orderId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentTransactionStatus status,
        String checkoutUrl,
        String qrCode,
        String externalReference,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDate businessDate,
        String failureReason) {
}