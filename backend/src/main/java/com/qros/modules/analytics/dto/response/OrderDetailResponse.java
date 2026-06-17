package com.qros.modules.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDetailResponse(
        Long orderId,
        LocalDateTime paymentTime,
        String userName,
        BigDecimal finalAmount,
        String tableNumber) {
}
