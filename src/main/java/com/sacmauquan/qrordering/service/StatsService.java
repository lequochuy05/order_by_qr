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
 * StatsService - Tổng hợp dữ liệu báo cáo kinh doanh Dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository repo;

    /**
     * Thống kê doanh thu theo ngày
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
     * Hiệu suất làm việc của nhân viên
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
     * Danh sách chi tiết các đơn hàng hoàn tất
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
     * Top món ăn bán chạy nhất
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
     * Xu hướng bán món ăn theo thời gian
     */
    public List<StatsResponse.DishTrend> getDishTrend(LocalDate from, LocalDate to) {
        return repo.dishTrendByDay(toStartOfInstant(from), toEndOfInstant(to)).stream()
                .map(r -> new StatsResponse.DishTrend(
                        r.getBucket(),
                        r.getTotalQty()))
                .collect(Collectors.toList());
    }

    // --- Helper Methods ---

    private Instant toStartOfInstant(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant toEndOfInstant(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
    }
}
