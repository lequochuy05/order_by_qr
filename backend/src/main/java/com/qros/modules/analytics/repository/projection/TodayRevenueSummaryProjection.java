package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;

public interface TodayRevenueSummaryProjection {

    BigDecimal getTodayRevenue();

    BigDecimal getTodayAvgOrderValue();
}
