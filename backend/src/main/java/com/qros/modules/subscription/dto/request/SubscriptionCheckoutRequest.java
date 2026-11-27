package com.qros.modules.subscription.dto.request;

import com.qros.shared.subscription.BillingCycle;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubscriptionCheckoutRequest(
        @NotNull(message = "Billing cycle is required") BillingCycle billingCycle,

        @Size(max = 200, message = "Return path cannot exceed 200 characters") String returnPath) {}
