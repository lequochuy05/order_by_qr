package com.qros.modules.analytics.dto.response;

import java.math.BigDecimal;

public record EmployeePerformanceResponse(
        Long staffId,
        String fullName,
        String avatarUrl,
        Long orderCount,
        BigDecimal revenue) {
}