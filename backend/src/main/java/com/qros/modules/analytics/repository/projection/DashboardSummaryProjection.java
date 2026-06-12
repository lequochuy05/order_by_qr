package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;

public interface DashboardSummaryProjection {

    BigDecimal getTotalRevenue();

    Long getTotalOrders();

    Long getTotalItemsSold();

    BigDecimal getAverageOrderValue();
}