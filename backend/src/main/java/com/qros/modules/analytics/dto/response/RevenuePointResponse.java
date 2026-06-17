package com.qros.modules.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenuePointResponse(LocalDate date, BigDecimal revenue, Long orderCount) {}
