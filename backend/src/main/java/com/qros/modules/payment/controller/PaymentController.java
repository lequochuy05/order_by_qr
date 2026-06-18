package com.qros.modules.payment.controller;

import com.qros.modules.payment.dto.request.PaymentCancelRequest;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.dto.response.PaymentTransactionResponse;
import com.qros.modules.payment.service.PaymentService;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiRoutes.PAYMENTS)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ApiResponse<PaymentCreateResponse> createPayment(
            @Valid @RequestBody @NonNull PaymentCreateRequest request, @NonNull Principal principal) {
        Long currentUserId = currentUserService.getCurrentUserId(principal.getName());
        return ApiResponse.success(
                "Payment initialized successfully", paymentService.createPayment(request, currentUserId));
    }

    @PostMapping("/{transactionId}/cancel")
    public ApiResponse<Void> cancelPaymentLink(
            @PathVariable @NonNull Long transactionId, @RequestBody(required = false) PaymentCancelRequest request) {

        String reason =
                request != null && request.reason() != null && !request.reason().isBlank()
                        ? request.reason().trim()
                        : "Customer changed payment method";

        paymentService.cancelPaymentLink(transactionId, reason);
        return ApiResponse.success("Transaction cancelled successfully", null);
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<PaymentTransactionResponse> syncPaymentStatus(@PathVariable @NonNull Long transactionId) {
        return ApiResponse.success(paymentService.syncPaymentStatus(transactionId));
    }
}
