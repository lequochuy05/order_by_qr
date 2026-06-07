package com.qros.modules.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderPayRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
    private String voucherCode;
}
