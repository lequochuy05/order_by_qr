package com.qros.modules.payment.controller;

import com.qros.shared.response.ApiResponse;
import com.qros.modules.payment.dto.PayosCreateRequest;
import com.qros.modules.payment.dto.PayosCreateResponse;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.service.PayosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PaymentController - Manages the integration with the PayOS payment gateway.
 */
@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
public class PaymentController {

    private final PayosService payosService;

    /**
     * Initializes a PayOS payment link (generates a payment QR code).
     * 
     * @param request Data required to create a payment link
     * @return PayosCreateResponse containing the payment URL and details
     */
    @PostMapping
    public ApiResponse<PayosCreateResponse> createPaymentLink(@Valid @RequestBody @NonNull PayosCreateRequest request) {
        return ApiResponse.success("Payment link initialized successfully", payosService.createPaymentLink(request));
    }

    /**
     * Cancels an existing payment link and records the reason for cancellation.
     * 
     * @param transactionId ID of the transaction to cancel
     * @param body Map containing the cancellation reason
     * @return Void success response
     */
    @PostMapping("/{transactionId}/cancellation")
    public ApiResponse<Void> cancelPaymentLink(
            @PathVariable @NonNull Long transactionId,
            @RequestBody @NonNull Map<String, String> body) {

        String reason = body.getOrDefault("reason", "Customer changed payment method");
        payosService.cancelPaymentLink(transactionId, reason);
        return ApiResponse.success("Transaction cancelled successfully", null);
    }

    /**
     * Synchronizes the actual transaction status from PayOS with the local system.
     * 
     * @param transactionId ID of the local transaction
     * @return PaymentTransaction object with updated status
     */
    @GetMapping("/{transactionId}")
    public ApiResponse<PaymentTransaction> syncPaymentStatus(@PathVariable @NonNull Long transactionId) {
        return ApiResponse.success(payosService.syncPaymentStatus(transactionId));
    }
}
