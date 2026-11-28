package com.qros.modules.subscription.dto.response;

import com.qros.shared.subscription.BillingCycle;
import com.qros.shared.subscription.PlanFeature;
import com.qros.shared.subscription.PlanTier;
import java.math.BigDecimal;
import java.util.List;

public record SubscriptionPlanResponse(
        String code,
        PlanTier planTier,
        BillingCycle billingCycle,
        BigDecimal priceVnd,
        String currency,
        Integer durationDays,
        Integer maxTables,
        Integer maxUsers,
        boolean recommended,
        List<PlanFeature> features) {

    public SubscriptionPlanResponse {
        features = features == null ? List.of() : List.copyOf(features);
    }
}
