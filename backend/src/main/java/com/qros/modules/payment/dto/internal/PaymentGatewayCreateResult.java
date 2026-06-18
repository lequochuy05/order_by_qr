package com.qros.modules.payment.dto.internal;

public record PaymentGatewayCreateResult(
        String checkoutUrl, String qrCode, String externalReference, String providerPayload) {}
