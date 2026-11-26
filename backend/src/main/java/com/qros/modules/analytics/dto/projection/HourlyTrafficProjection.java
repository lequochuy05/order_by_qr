package com.qros.modules.analytics.dto.projection;

import java.math.BigDecimal;

public interface HourlyTrafficProjection {

    Integer getHourOfDay();

    Long getOrderCount();

    BigDecimal getRevenue();
}
