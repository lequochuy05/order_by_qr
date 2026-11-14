package com.qros.modules.payment.dto.internal;

import java.math.BigDecimal;

public record PaymentWebhookResult(
        Long gatewayOrderCode, BigDecimal amount, String externalReference, String providerPayload) {}
