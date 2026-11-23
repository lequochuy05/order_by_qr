package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;

public interface DayOfWeekTrafficProjection {

    Integer getIsoDayOfWeek();

    Long getOrderCount();

    BigDecimal getRevenue();
}
