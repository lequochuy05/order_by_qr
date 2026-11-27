package com.qros.modules.analytics.dto.projection;

import java.math.BigDecimal;

public interface DayOfWeekTrafficProjection {

    Integer getIsoDayOfWeek();

    Long getOrderCount();

    BigDecimal getRevenue();
}
