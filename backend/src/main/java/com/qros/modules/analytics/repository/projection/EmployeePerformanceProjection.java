package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;

public interface EmployeePerformanceProjection {

    Long getStaffId();

    String getFullName();

    String getAvatarUrl();

    Long getOrderCount();

    BigDecimal getRevenue();
}