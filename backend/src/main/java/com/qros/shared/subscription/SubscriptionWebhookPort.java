package com.qros.shared.subscription;

public interface SubscriptionWebhookPort {

    boolean tryHandleWebhook(SubscriptionWebhookPayload payload);
}
