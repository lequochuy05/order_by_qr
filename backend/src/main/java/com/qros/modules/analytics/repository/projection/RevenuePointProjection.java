package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;

public interface RevenuePointProjection {

    String getDate();

    BigDecimal getRevenue();

    Long getOrderCount();
}