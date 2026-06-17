package com.qros.modules.analytics.dto.response;

import java.math.BigDecimal;

public record UserPerformanceResponse(
        Long userId,
        String fullName,
        String avatarUrl,
        Long orderCount,
        BigDecimal revenue) {
}
