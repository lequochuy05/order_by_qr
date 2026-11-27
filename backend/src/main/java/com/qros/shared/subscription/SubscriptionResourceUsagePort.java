package com.qros.shared.subscription;

public interface SubscriptionResourceUsagePort {

    enum ResourceType {
        ACTIVE_TABLES,
        ACTIVE_USERS
    }

    ResourceType getResourceType();

    long countActive();
}
