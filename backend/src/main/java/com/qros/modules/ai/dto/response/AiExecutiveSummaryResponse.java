package com.qros.modules.ai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AiExecutiveSummaryResponse(
        LocalDate from,
        LocalDate to,
        String language,
        String headline,
        List<String> highlights,
        List<AnomalyAlert> anomalies,
        List<String> recommendations,
        MetricsSnapshot metrics,
        LocalDateTime generatedAt,
        boolean aiGenerated) {

    public AiExecutiveSummaryResponse {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }

    public record AnomalyAlert(String severity, String title, String detail) {}

    public record TopDishMetric(String name, long quantitySold, BigDecimal revenue) {}

    public record MetricsSnapshot(
            BigDecimal revenue,
            BigDecimal previousRevenue,
            Double revenueChangePercent,
            long completedOrders,
            long previousCompletedOrders,
            long cancelledOrders,
            BigDecimal averageOrderValue,
            long totalItemsSold,
            long occupiedTables,
            long totalTables,
            long activeKitchenItems,
            long overdueKitchenItems,
            long maxKitchenWaitMinutes,
            long totalServiceRequests,
            long openServiceRequests,
            long overdueServiceRequests,
            String topServiceRequestReason,
            long lowStockItems,
            long outOfStockItems,
            List<String> lowStockItemNames,
            List<TopDishMetric> topDishes) {

        public MetricsSnapshot {
            lowStockItemNames = lowStockItemNames == null ? List.of() : List.copyOf(lowStockItemNames);
            topDishes = topDishes == null ? List.of() : List.copyOf(topDishes);
        }
    }
}
