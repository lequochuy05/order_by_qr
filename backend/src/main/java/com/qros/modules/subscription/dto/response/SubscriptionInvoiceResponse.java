package com.qros.modules.subscription.dto.response;

import com.qros.modules.subscription.model.SubscriptionInvoiceStatus;
import com.qros.shared.subscription.BillingCycle;
import com.qros.shared.subscription.PlanTier;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionInvoiceResponse(
        Long id,
        String invoiceCode,
        PlanTier planTier,
        BillingCycle billingCycle,
        BigDecimal amount,
        String currency,
        SubscriptionInvoiceStatus status,
        Long gatewayOrderCode,
        String paymentLinkId,
        String checkoutUrl,
        String qrCode,
        String createdByEmail,
        LocalDateTime paidAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {}
