package com.qros.modules.payment.dto.internal;

import com.qros.modules.payment.model.enums.PaymentTransactionStatus;

public record PaymentGatewayStatusResult(
        PaymentTransactionStatus status,
        String externalReference,
        String providerPayload,
        String failureReason) {
}