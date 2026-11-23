package com.qros.modules.payment.controller;

import com.qros.modules.payment.dto.request.CustomerSelfCheckoutCancelRequest;
import com.qros.modules.payment.dto.request.CustomerSelfCheckoutCreateRequest;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.dto.response.PaymentTransactionResponse;
import com.qros.modules.payment.service.CustomerSelfCheckoutService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiRoutes.PUBLIC)
@RequiredArgsConstructor
public class PublicSelfCheckoutController {

    private final CustomerSelfCheckoutService selfCheckoutService;

    @PostMapping("/tables/{tableCode}/self-checkout")
    public ApiResponse<PaymentCreateResponse> createSelfCheckout(
            @PathVariable @NonNull String tableCode,
            @Valid @RequestBody @NonNull CustomerSelfCheckoutCreateRequest request) {
        return ApiResponse.success(
                "Self-checkout payment initialized successfully",
                selfCheckoutService.createSelfCheckout(tableCode, request));
    }

    @PostMapping("/tables/{tableCode}/self-checkout/{transactionId}/cancel")
    public ApiResponse<Void> cancelSelfCheckout(
            @PathVariable @NonNull String tableCode,
            @PathVariable @NonNull Long transactionId,
            @Valid @RequestBody @NonNull CustomerSelfCheckoutCancelRequest request) {
        selfCheckoutService.cancelSelfCheckout(tableCode, transactionId, request);
        return ApiResponse.success("Self-checkout transaction cancelled successfully", null);
    }

    @GetMapping("/tables/{tableCode}/self-checkout/{transactionId}")
    public ApiResponse<PaymentTransactionResponse> syncSelfCheckoutStatus(
            @PathVariable @NonNull String tableCode,
            @PathVariable @NonNull Long transactionId,
            @RequestParam @NotBlank(message = "Session token is required") String sessionToken) {
        return ApiResponse.success(selfCheckoutService.syncSelfCheckoutStatus(tableCode, transactionId, sessionToken));
    }
}
