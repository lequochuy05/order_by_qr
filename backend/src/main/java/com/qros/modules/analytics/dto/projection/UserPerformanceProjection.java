package com.qros.modules.analytics.dto.projection;

import java.math.BigDecimal;

public interface UserPerformanceProjection {

    Long getUserId();

    String getFullName();

    String getAvatarUrl();

    Long getOrderCount();

    BigDecimal getRevenue();
}
