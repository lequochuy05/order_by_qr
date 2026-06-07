package com.qros.modules.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard Stats Response - Data transfer objects for reporting and
 * statistics.
 */
public class StatsResponse {

        public record Revenue(
                        String bucket,
                        BigDecimal revenue,
                        Long orders) {
        }

        public record EmpPerformance(
                        Long id,
                        String fullName,
                        String avatarUrl,
                        Long orders,
                        BigDecimal revenue) {
        }

        public record OrderDetail(
                        Long id,
                        LocalDateTime paymentTime,
                        String empName,
                        BigDecimal finalAmount,
                        String tableNumber) {
        }

        public record TopDish(
                        Long id,
                        String name,
                        String category,
                        String img,
                        Long totalQty,
                        BigDecimal totalRevenue) {
        }

        public record DishTrend(
                        String bucket,
                        Long totalQty) {
        }

        public record RevenueForecast(
                        LocalDate date,
                        BigDecimal actual,
                        BigDecimal forecast,
                        boolean forecasted) {
        }

        public record PopularDishForecast(
                        Long id,
                        String name,
                        String category,
                        Long estimatedQty) {
        }

        /**
         * Composite response for the admin dashboard — aggregates all stats into a single payload.
         * Reduces frontend round-trips from 7 API calls to 1.
         */
        public record DashboardSummary(
                        List<Revenue> revenue,
                        List<EmpPerformance> employees,
                        List<OrderDetail> orders,
                        List<TopDish> topDishes,
                        List<DishTrend> dishTrend,
                        List<RevenueForecast> revenueForecast,
                        List<PopularDishForecast> popularDishesForecast) {
        }
}
