package com.qros.modules.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerSelfCheckoutCreateRequest(
        @NotBlank(message = "Session token is required") String sessionToken,

        @Size(max = 50, message = "Voucher code must not exceed 50 characters") String voucherCode,

        @Size(max = 100, message = "Idempotency key must not exceed 100 characters") String idempotencyKey) {}
