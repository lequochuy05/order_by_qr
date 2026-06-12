package com.qros.modules.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecentOrderResponse(
        Long orderId,
        String status,
        BigDecimal finalAmount,
        LocalDateTime createdAt,
        LocalDateTime paymentTime,
        String tableNumber) {
}