package com.qros.modules.analytics.service;

import com.qros.modules.analytics.dto.projection.DashboardSummaryProjection;
import com.qros.modules.analytics.dto.projection.DayOfWeekTrafficProjection;
import com.qros.modules.analytics.dto.projection.HourlyTrafficProjection;
import com.qros.modules.analytics.dto.projection.MenuItemDemandProjection;
import com.qros.modules.analytics.dto.projection.OrderFilterSummaryProjection;
import com.qros.modules.analytics.dto.projection.TableSummaryProjection;
import com.qros.modules.analytics.dto.projection.TopSellingItemProjection;
import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Module-boundary lookup facade for analytics query projections consumed by other modules.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsLookupService {

    private final AnalyticsQueryRepository analyticsQueryRepository;

    public DashboardSummaryProjection dashboardSummary(LocalDate from, LocalDate toExclusive) {
        return analyticsQueryRepository.dashboardSummary(from, toExclusive);
    }

    public OrderFilterSummaryProjection orderFilterSummary(
            String status, LocalDate from, LocalDate toExclusive, Long orderId, String tableNumber) {
        return analyticsQueryRepository.orderFilterSummary(status, from, toExclusive, orderId, tableNumber);
    }

    public TableSummaryProjection tableSummary() {
        return analyticsQueryRepository.tableSummary();
    }

    public List<TopSellingItemProjection> topSellingItems(LocalDate from, LocalDate toExclusive, int limit) {
        return analyticsQueryRepository.topSellingItems(from, toExclusive, limit);
    }

    public List<MenuItemDemandProjection> menuItemDemandIncludingCombos(LocalDate from, LocalDate toExclusive) {
        return analyticsQueryRepository.menuItemDemandIncludingCombos(from, toExclusive);
    }

    public List<HourlyTrafficProjection> hourlyOrderTraffic(LocalDate from, LocalDate toExclusive) {
        return analyticsQueryRepository.hourlyOrderTraffic(from, toExclusive);
    }

    public List<DayOfWeekTrafficProjection> dayOfWeekOrderTraffic(LocalDate from, LocalDate toExclusive) {
        return analyticsQueryRepository.dayOfWeekOrderTraffic(from, toExclusive);
    }
}
