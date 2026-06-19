package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;

public interface OrderFilterSummaryProjection {

    Long getTotalOrders();

    BigDecimal getTotalRevenue();
}
