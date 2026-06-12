package com.qros.modules.payment.controller;

import com.qros.modules.payment.dto.request.PaymentCancelRequest;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.dto.response.PaymentTransactionResponse;
import com.qros.modules.payment.service.PaymentService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentCreateResponse> createPaymentLink(
            @Valid @RequestBody @NonNull PaymentCreateRequest request) {
        return ApiResponse.success(
                "Payment link initialized successfully",
                paymentService.createPaymentLink(request));
    }

    @PostMapping("/{transactionId}/cancel")
    public ApiResponse<Void> cancelPaymentLink(
            @PathVariable @NonNull Long transactionId,
            @RequestBody(required = false) PaymentCancelRequest request) {

        String reason = request != null && request.reason() != null && !request.reason().isBlank()
                ? request.reason().trim()
                : "Customer changed payment method";

        paymentService.cancelPaymentLink(transactionId, reason);
        return ApiResponse.success("Transaction cancelled successfully", null);
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<PaymentTransactionResponse> syncPaymentStatus(
            @PathVariable @NonNull Long transactionId) {
        return ApiResponse.success(paymentService.syncPaymentStatus(transactionId));
    }
}