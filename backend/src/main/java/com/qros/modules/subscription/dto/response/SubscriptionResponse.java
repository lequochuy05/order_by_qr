package com.qros.modules.subscription.dto.response;

import com.qros.shared.subscription.BillingCycle;
import com.qros.shared.subscription.PlanFeature;
import com.qros.shared.subscription.PlanTier;
import com.qros.shared.subscription.SubscriptionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SubscriptionResponse(
        PlanTier planTier,
        SubscriptionStatus status,
        BillingCycle billingCycle,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        boolean trialUsed,
        boolean proActive,
        boolean enforcementEnabled,
        List<PlanFeature> enabledFeatures,
        ResourceUsage usage) {

    public SubscriptionResponse {
        enabledFeatures = enabledFeatures == null ? List.of() : List.copyOf(enabledFeatures);
    }

    public record ResourceUsage(long activeTables, Integer maxTables, long activeUsers, Integer maxUsers) {}
}
