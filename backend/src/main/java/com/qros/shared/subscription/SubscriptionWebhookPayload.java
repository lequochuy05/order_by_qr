package com.qros.shared.subscription;

import java.math.BigDecimal;

public record SubscriptionWebhookPayload(
        Long gatewayOrderCode, BigDecimal amount, String externalReference, String providerPayload) {}
