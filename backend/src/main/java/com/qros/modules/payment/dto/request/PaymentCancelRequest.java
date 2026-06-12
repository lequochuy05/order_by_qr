package com.qros.modules.payment.dto.request;

import jakarta.validation.constraints.Size;

public record PaymentCancelRequest(

        @Size(max = 255, message = "Reason must not exceed 255 characters") String reason) {
}