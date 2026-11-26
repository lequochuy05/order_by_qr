package com.qros.modules.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerSelfCheckoutCancelRequest(
        @NotBlank(message = "Session token is required") String sessionToken,

        @Size(max = 255, message = "Cancellation reason must not exceed 255 characters") String reason) {}
