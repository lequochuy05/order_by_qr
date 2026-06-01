package com.sacmauquan.qrordering.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
                        Long orders,
                        BigDecimal revenue) {
        }

        public record OrderDetail(
                        Long id,
                        Instant paymentTime,
                        String empName,
                        BigDecimal totalAmount) {
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
}
