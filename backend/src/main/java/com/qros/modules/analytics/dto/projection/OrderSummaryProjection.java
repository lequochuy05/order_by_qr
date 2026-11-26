package com.qros.modules.analytics.dto.projection;

public interface OrderSummaryProjection {

    Long getTotalOrders();

    Long getCompletedOrders();
}
