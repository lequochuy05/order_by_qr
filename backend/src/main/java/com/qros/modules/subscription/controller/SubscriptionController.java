package com.qros.modules.subscription.controller;

import com.qros.modules.subscription.dto.request.SubscriptionCheckoutRequest;
import com.qros.modules.subscription.dto.response.SubscriptionInvoiceResponse;
import com.qros.modules.subscription.dto.response.SubscriptionPlanResponse;
import com.qros.modules.subscription.dto.response.SubscriptionResponse;
import com.qros.modules.subscription.service.SubscriptionService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.SUBSCRIPTION)
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    public ApiResponse<SubscriptionResponse> getCurrentSubscription() {
        return ApiResponse.success(subscriptionService.getCurrentSubscription());
    }

    @GetMapping("/plans")
    public ApiResponse<List<SubscriptionPlanResponse>> getAvailablePlans() {
        return ApiResponse.success(subscriptionService.getAvailablePlans());
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<List<SubscriptionInvoiceResponse>> listInvoices() {
        return ApiResponse.success(subscriptionService.listInvoices());
    }

    @PostMapping("/trial")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubscriptionResponse> activateTrial(@NonNull Principal principal) {
        return ApiResponse.success(subscriptionService.activateTrial(principal.getName()));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubscriptionInvoiceResponse> createCheckout(
            @Valid @RequestBody SubscriptionCheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @NonNull Principal principal) {
        return ApiResponse.success(subscriptionService.createCheckout(request, idempotencyKey, principal.getName()));
    }

    @PostMapping("/invoices/{invoiceId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubscriptionInvoiceResponse> syncInvoice(@PathVariable @NonNull Long invoiceId) {
        return ApiResponse.success(subscriptionService.syncInvoiceStatus(invoiceId));
    }

    @PostMapping("/invoices/{invoiceId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubscriptionInvoiceResponse> cancelInvoice(@PathVariable @NonNull Long invoiceId) {
        return ApiResponse.success(subscriptionService.cancelInvoice(invoiceId));
    }
}
