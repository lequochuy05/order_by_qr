package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.StatsResponse;
import com.sacmauquan.qrordering.repository.StatsRepository;
import com.sacmauquan.qrordering.util.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        @Cacheable(value = "stats_revenue", key = "#from.toString() + '_' + #to.toString()")
        public List<StatsResponse.Revenue> getRevenue(LocalDate from, LocalDate to) {
                return repo.revenueByDay(toStartOfDay(from), toEndOfDay(to)).stream()
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
        @Cacheable(value = "stats_emp_performance", key = "#from.toString() + '_' + #to.toString()")
        public List<StatsResponse.EmpPerformance> getEmployeePerformance(LocalDate from, LocalDate to) {
                return repo.empPerformance(toStartOfDay(from), toEndOfDay(to)).stream()
                                .map(r -> new StatsResponse.EmpPerformance(
                                                r.getId(),
                                                r.getFullName(),
                                                r.getAvatarUrl(),
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
                return repo.orderDetails(toStartOfDay(from), toEndOfDay(to)).stream()
                                .map(r -> new StatsResponse.OrderDetail(
                                                r.getId(),
                                                r.getPaymentTime(),
                                                r.getEmpName(),
                                                r.getTotalAmount(),
                                                r.getTableNumber()))
                                .collect(Collectors.toList());
        }

        /**
         * Identifies top-selling dishes based on sales volume and revenue.
         * 
         * @param from Start date
         * @param to End date
         * @return List of popular menu items
         */
        @Cacheable(value = "stats_top_dishes", key = "#from.toString() + '_' + #to.toString()")
        public List<StatsResponse.TopDish> getTopDishes(LocalDate from, LocalDate to) {
                return repo.topDishes(toStartOfDay(from), toEndOfDay(to)).stream()
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
        @Cacheable(value = "stats_dish_trend", key = "#from.toString() + '_' + #to.toString()")
        public List<StatsResponse.DishTrend> getDishTrend(LocalDate from, LocalDate to) {
                return repo.dishTrendByDay(toStartOfDay(from), toEndOfDay(to)).stream()
                                .map(r -> new StatsResponse.DishTrend(
                                                r.getBucket(),
                                                r.getTotalQty()))
                                .collect(Collectors.toList());
        }

        /**
         * Builds a simple 30-day actual revenue + 7-day forecast series for the dashboard.
         */
        public List<StatsResponse.RevenueForecast> getRevenueForecast() {
                LocalDate today = AppTime.today();
                LocalDate from = today.minusDays(29);

                Map<LocalDate, BigDecimal> actualByDate = new HashMap<>();
                repo.revenueByDay(toStartOfDay(from), toEndOfDay(today)).forEach(row -> {
                        if (row.getBucket() != null) {
                                actualByDate.put(LocalDate.parse(row.getBucket()),
                                                row.getRevenue() != null ? row.getRevenue() : BigDecimal.ZERO);
                        }
                });

                BigDecimal total = actualByDate.values().stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal average = total.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

                List<StatsResponse.RevenueForecast> result = new ArrayList<>();
                for (int i = 0; i < 30; i++) {
                        LocalDate date = from.plusDays(i);
                        result.add(new StatsResponse.RevenueForecast(
                                        date,
                                        actualByDate.getOrDefault(date, BigDecimal.ZERO),
                                        null,
                                        false));
                }

                for (int i = 1; i <= 7; i++) {
                        result.add(new StatsResponse.RevenueForecast(
                                        today.plusDays(i),
                                        null,
                                        average,
                                        true));
                }

                return result;
        }

        /**
         * Estimates next-week popular dishes from the latest 30 days of completed orders.
         */
        public List<StatsResponse.PopularDishForecast> getPopularDishesForecast() {
                LocalDate today = AppTime.today();
                LocalDate from = today.minusDays(29);

                return repo.topDishes(toStartOfDay(from), toEndOfDay(today)).stream()
                                .limit(5)
                                .map(row -> {
                                        long totalQty = row.getTotalQty() != null ? row.getTotalQty() : 0L;
                                        long estimatedQty = Math.max(1L, Math.round(totalQty * 7.0 / 30.0));
                                        return new StatsResponse.PopularDishForecast(
                                                        row.getId(),
                                                        row.getName(),
                                                        row.getCategory(),
                                                        estimatedQty);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Composite endpoint — aggregates all dashboard stats into a single response.
         * Reduces 7 frontend HTTP round-trips to 1.
         * 
         * @param from Start date for date-range stats
         * @param to End date for date-range stats
         * @return DashboardSummary containing all dashboard data
         */
        @Cacheable(value = "stats_dashboard", key = "#from.toString() + '_' + #to.toString()")
        public StatsResponse.DashboardSummary getDashboardSummary(LocalDate from, LocalDate to) {
                return new StatsResponse.DashboardSummary(
                        getRevenue(from, to),
                        getEmployeePerformance(from, to),
                        getOrderDetails(from, to),
                        getTopDishes(from, to),
                        getDishTrend(from, to),
                        getRevenueForecast(),
                        getPopularDishesForecast()
                );
        }

        /**
         * Converts LocalDate to the start of the business day in Vietnam time.
         */
        private LocalDateTime toStartOfDay(LocalDate date) {
                return date.atStartOfDay();
        }

        /**
         * Converts LocalDate to the precise end of the business day in Vietnam time.
         */
        private LocalDateTime toEndOfDay(LocalDate date) {
                return date.plusDays(1).atStartOfDay().minusNanos(1);
        }
}
