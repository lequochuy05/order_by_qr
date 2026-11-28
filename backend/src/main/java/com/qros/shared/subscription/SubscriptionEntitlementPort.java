package com.qros.shared.subscription;

public interface SubscriptionEntitlementPort {

    boolean isFeatureEnabled(PlanFeature feature);

    void assertFeatureEnabled(PlanFeature feature);

    void assertTableLimitNotExceeded(long currentActiveTables);

    void assertUserLimitNotExceeded(long currentActiveUsers);
}
