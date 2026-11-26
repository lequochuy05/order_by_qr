package com.qros.modules.analytics.dto.projection;

import java.math.BigDecimal;

public interface OrderFilterSummaryProjection {

    Long getTotalOrders();

    BigDecimal getTotalRevenue();
}
