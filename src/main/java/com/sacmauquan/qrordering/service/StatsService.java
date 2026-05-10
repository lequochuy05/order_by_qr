package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.StatsResponse;
import com.sacmauquan.qrordering.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StatsService - Analytical service for generating business reports and dashboard metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

        private final StatsRepository repo;

        /**
         * Retrieves daily revenue and order counts for a specified period.
         * 
         * @param from Start date
         * @param to End date
         * @return List of daily revenue statistics
         */
        @Cacheable(value = "stats_revenue", key = "#from.toString() + #to.toString()")
        public List<StatsResponse.Revenue> getRevenue(LocalDate from, LocalDate to) {
                return repo.revenueByDay(toStartOfInstant(from), toEndOfInstant(to)).stream()
                                .map(r -> new StatsResponse.Revenue(
                                                r.getBucket(),
                                                r.getRevenue(),
                                                r.getOrders()))
                                .collect(Collectors.toList());
        }

        /**
         * Evaluates employee performance based on orders handled and revenue generated.
         * 
         * @param from Start date
         * @param to End date
         * @return List of employee performance metrics
         */
        public List<StatsResponse.EmpPerformance> getEmployeePerformance(LocalDate from, LocalDate to) {
                return repo.empPerformance(toStartOfInstant(from), toEndOfInstant(to)).stream()
                                .map(r -> new StatsResponse.EmpPerformance(
                                                r.getId(),
                                                r.getFullName(),
                                                r.getOrders(),
                                                r.getRevenue()))
                                .collect(Collectors.toList());
        }

        /**
         * Retrieves detailed information for all completed orders within a date range.
         * 
         * @param from Start date
         * @param to End date
         * @return List of order summaries
         */
        public List<StatsResponse.OrderDetail> getOrderDetails(LocalDate from, LocalDate to) {
                return repo.orderDetails(toStartOfInstant(from), toEndOfInstant(to)).stream()
                                .map(r -> new StatsResponse.OrderDetail(
                                                r.getId(),
                                                r.getPaymentTime(),
                                                r.getEmpName(),
                                                r.getTotalAmount()))
                                .collect(Collectors.toList());
        }

        /**
         * Identifies top-selling dishes based on sales volume and revenue.
         * 
         * @param from Start date
         * @param to End date
         * @return List of popular menu items
         */
        @Cacheable(value = "stats_top_dishes", key = "#from.toString() + #to.toString()")
        public List<StatsResponse.TopDish> getTopDishes(LocalDate from, LocalDate to) {
                return repo.topDishes(toStartOfInstant(from), toEndOfInstant(to)).stream()
                                .map(r -> new StatsResponse.TopDish(
                                                r.getId(),
                                                r.getName(),
                                                r.getCategory(),
                                                r.getImg(),
                                                r.getTotalQty(),
                                                r.getTotalRevenue()))
                                .collect(Collectors.toList());
        }

        /**
         * Analyzes daily sales trends for menu items.
         * 
         * @param from Start date
         * @param to End date
         * @return List of daily sales quantity trends
         */
        public List<StatsResponse.DishTrend> getDishTrend(LocalDate from, LocalDate to) {
                return repo.dishTrendByDay(toStartOfInstant(from), toEndOfInstant(to)).stream()
                                .map(r -> new StatsResponse.DishTrend(
                                                r.getBucket(),
                                                r.getTotalQty()))
                                .collect(Collectors.toList());
        }

        /**
         * Converts LocalDate to an Instant at the start of the day in system default timezone.
         */
        private Instant toStartOfInstant(LocalDate date) {
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }

        /**
         * Converts LocalDate to an Instant at the precise end of the day in system default timezone.
         */
        private Instant toEndOfInstant(LocalDate date) {
                return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        }
}
