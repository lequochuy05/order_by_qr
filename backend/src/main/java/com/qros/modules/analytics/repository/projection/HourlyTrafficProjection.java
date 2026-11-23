package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;

public interface HourlyTrafficProjection {

    Integer getHourOfDay();

    Long getOrderCount();

    BigDecimal getRevenue();
}
