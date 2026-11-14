package com.qros.modules.analytics.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        BigDecimal totalRevenue,
        Long totalOrders,
        Long totalItemsSold,
        BigDecimal averageOrderValue,
        BigDecimal todayRevenue,
        BigDecimal todayAvgOrderValue,
        List<RevenuePointResponse> revenue,
        List<UserPerformanceResponse> users,
        List<OrderDetailResponse> recentCompletedOrders,
        List<TopSellingItemResponse> topItems,
        List<SalesTrendPointResponse> salesTrend,
        List<RevenueForecastResponse> revenueForecast,
        List<PopularItemForecastResponse> popularItemsForecast,
        OrderSummaryResponse todayOrders,
        TableSummaryResponse tables,
        List<RecentOrderResponse> recentOrders) {}
