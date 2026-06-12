package com.qros.modules.analytics.repository.projection;

public interface OrderSummaryProjection {

    Long getTotalOrders();

    Long getCompletedOrders();
}