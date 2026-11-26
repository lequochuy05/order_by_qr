package com.qros.modules.analytics.dto.projection;

import java.math.BigDecimal;

public interface RevenuePointProjection {

    String getDate();

    BigDecimal getRevenue();

    Long getOrderCount();
}
