package com.qros.modules.analytics.dto.projection;

import java.math.BigDecimal;

public interface TodayRevenueSummaryProjection {

    BigDecimal getTodayRevenue();

    BigDecimal getTodayAvgOrderValue();
}
