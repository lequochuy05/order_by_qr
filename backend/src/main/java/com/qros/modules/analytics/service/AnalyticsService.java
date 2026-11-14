package com.qros.modules.analytics.service;

import com.qros.modules.analytics.dto.response.DashboardSummaryResponse;
import com.qros.modules.analytics.dto.response.OrderDetailResponse;
import com.qros.modules.analytics.dto.response.OrderSummaryResponse;
import com.qros.modules.analytics.dto.response.PopularItemForecastResponse;
import com.qros.modules.analytics.dto.response.RecentOrderResponse;
import com.qros.modules.analytics.dto.response.RevenueForecastResponse;
import com.qros.modules.analytics.dto.response.RevenuePointResponse;
import com.qros.modules.analytics.dto.response.SalesTrendPointResponse;
import com.qros.modules.analytics.dto.response.TableSummaryResponse;
import com.qros.modules.analytics.dto.response.TopSellingItemResponse;
import com.qros.modules.analytics.dto.response.UserPerformanceResponse;
import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import com.qros.modules.analytics.repository.projection.DashboardSummaryProjection;
import com.qros.modules.analytics.repository.projection.OrderFilterSummaryProjection;
import com.qros.modules.analytics.repository.projection.OrderSummaryProjection;
import com.qros.modules.analytics.repository.projection.TableSummaryProjection;
import com.qros.modules.analytics.repository.projection.TodayRevenueSummaryProjection;
import com.qros.modules.analytics.repository.projection.UserPerformanceProjection;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int DEFAULT_DAYS = 30;
    private static final int DASHBOARD_DAYS = 7;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final AnalyticsQueryRepository analyticsQueryRepository;
    private final AnalyticsForecastService analyticsForecastService;
    private final OrderService orderService;

    @Cacheable(value = CacheNames.ANALYTICS, key = "'dashboard:' + #from + ':' + #to")
    public DashboardSummaryResponse getDashboardSummary(LocalDate from, LocalDate to) {
        DateRange range = normalizeDateRange(from, to, DASHBOARD_DAYS);

        DashboardSummaryProjection summary =
                analyticsQueryRepository.dashboardSummary(range.from(), range.toExclusive());

        LocalDate today = AppTime.now().toLocalDate();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        OrderSummaryProjection orderSummary = analyticsQueryRepository.orderSummary(today, today.plusDays(1));

        TableSummaryProjection tableSummary = analyticsQueryRepository.tableSummary();

        TodayRevenueSummaryProjection todayRevenueSummary = analyticsQueryRepository.todayRevenueSummary(today);

        return new DashboardSummaryResponse(
                safeMoney(summary != null ? summary.getTotalRevenue() : null),
                safeLong(summary != null ? summary.getTotalOrders() : null),
                safeLong(summary != null ? summary.getTotalItemsSold() : null),
                safeMoney(summary != null ? summary.getAverageOrderValue() : null),
                safeMoney(todayRevenueSummary != null ? todayRevenueSummary.getTodayRevenue() : null),
                safeMoney(todayRevenueSummary != null ? todayRevenueSummary.getTodayAvgOrderValue() : null),
                getRevenueSeries(range.from(), range.to()),
                getUserPerformance(range.from(), range.to(), 5),
                getOrderDetails(range.from(), range.to(), PageRequest.of(0, 5)).getContent(),
                getTopSellingItems(range.from(), range.to(), 5),
                getSalesTrend(range.from(), range.to()),
                analyticsForecastService.getRevenueForecast(),
                analyticsForecastService.getPopularItemsForecast(),
                toOrderSummaryResponse(orderSummary),
                toTableSummaryResponse(tableSummary),
                getRecentOrders(todayStart, tomorrowStart, 5));
    }

    @Cacheable(value = CacheNames.ANALYTICS, key = "'revenue:' + #from + ':' + #to")
    public List<RevenuePointResponse> getRevenueSeries(LocalDate from, LocalDate to) {
        DateRange range = normalizeDateRange(from, to, DEFAULT_DAYS);

        return analyticsQueryRepository.revenueByDay(range.from(), range.toExclusive()).stream()
                .map(row -> new RevenuePointResponse(
                        LocalDate.parse(row.getDate()), safeMoney(row.getRevenue()), safeLong(row.getOrderCount())))
                .toList();
    }

    @Cacheable(value = CacheNames.ANALYTICS, key = "'users:' + #from + ':' + #to + ':' + #limit")
    public List<UserPerformanceResponse> getUserPerformance(LocalDate from, LocalDate to, int limit) {
        DateRange range = normalizeDateRange(from, to, DEFAULT_DAYS);
        int safeLimit = sanitizeLimit(limit);

        return analyticsQueryRepository.userPerformance(range.from(), range.toExclusive(), safeLimit).stream()
                .map(this::toUserPerformanceResponse)
                .toList();
    }

    public Page<OrderDetailResponse> getOrderDetails(LocalDate from, LocalDate to, Pageable pageable) {
        DateRange range = normalizeDateRange(from, to, DEFAULT_DAYS);

        return analyticsQueryRepository
                .orderDetails(range.from(), range.toExclusive(), pageable)
                .map(row -> new OrderDetailResponse(
                        row.getOrderId(),
                        row.getPaymentTime(),
                        row.getUserName(),
                        safeMoney(row.getFinalAmount()),
                        row.getTableNumber()));
    }

    @Cacheable(value = CacheNames.ANALYTICS, key = "'topItems:' + #from + ':' + #to + ':' + #limit")
    public List<TopSellingItemResponse> getTopSellingItems(LocalDate from, LocalDate to, int limit) {
        DateRange range = normalizeDateRange(from, to, DEFAULT_DAYS);
        int safeLimit = sanitizeLimit(limit);

        return analyticsQueryRepository.topSellingItems(range.from(), range.toExclusive(), safeLimit).stream()
                .map(row -> new TopSellingItemResponse(
                        row.getMenuItemId(),
                        row.getItemName(),
                        row.getCategoryName(),
                        row.getImageUrl(),
                        safeLong(row.getQuantitySold()),
                        safeMoney(row.getRevenue())))
                .toList();
    }

    @Cacheable(value = CacheNames.ANALYTICS, key = "'salesTrend:' + #from + ':' + #to")
    public List<SalesTrendPointResponse> getSalesTrend(LocalDate from, LocalDate to) {
        DateRange range = normalizeDateRange(from, to, DEFAULT_DAYS);

        return analyticsQueryRepository.salesTrendByDay(range.from(), range.toExclusive()).stream()
                .map(row ->
                        new SalesTrendPointResponse(LocalDate.parse(row.getDate()), safeLong(row.getQuantitySold())))
                .toList();
    }

    /**
     * Delegate để controller có thể vẫn chỉ inject AnalyticsService.
     */
    public List<RevenueForecastResponse> getRevenueForecast() {
        return analyticsForecastService.getRevenueForecast();
    }

    /**
     * Delegate để controller có thể vẫn chỉ inject AnalyticsService.
     */
    public List<PopularItemForecastResponse> getPopularItemsForecast() {
        return analyticsForecastService.getPopularItemsForecast();
    }

    public Page<OrderResponse> getOrderHistory(
            String status,
            LocalDate from,
            LocalDate to,
            String orderId,
            String tableNumber,
            @NonNull Pageable pageable) {
        return orderService.getOrderHistory(status, from, to, orderId, tableNumber, pageable);
    }

    public Map<String, Object> getOrderAnalytics(
            String status, LocalDate from, LocalDate to, String orderId, String tableNumber) {
        Long parsedOrderId = parseOrderId(orderId);
        if (orderId != null && !orderId.isBlank() && parsedOrderId == null) {
            return orderSummaryMap(0L, BigDecimal.ZERO);
        }

        OrderFilterSummaryProjection summary = analyticsQueryRepository.orderFilterSummary(
                normalizeStatus(status),
                from,
                to != null ? to.plusDays(1) : null,
                parsedOrderId,
                normalizeText(tableNumber));

        return orderSummaryMap(
                safeLong(summary != null ? summary.getTotalOrders() : null),
                safeMoney(summary != null ? summary.getTotalRevenue() : null));
    }

    private List<RecentOrderResponse> getRecentOrders(
            @NonNull LocalDateTime from, @NonNull LocalDateTime toExclusive, int limit) {
        return analyticsQueryRepository.recentOrders(from, toExclusive, sanitizeLimit(limit)).stream()
                .map(row -> new RecentOrderResponse(
                        row.getOrderId(),
                        row.getStatus(),
                        safeMoney(row.getFinalAmount()),
                        row.getCreatedAt(),
                        row.getPaymentTime(),
                        row.getTableNumber()))
                .toList();
    }

    private UserPerformanceResponse toUserPerformanceResponse(UserPerformanceProjection projection) {
        return new UserPerformanceResponse(
                projection.getUserId(),
                projection.getFullName(),
                projection.getAvatarUrl(),
                safeLong(projection.getOrderCount()),
                safeMoney(projection.getRevenue()));
    }

    private OrderSummaryResponse toOrderSummaryResponse(OrderSummaryProjection projection) {
        return new OrderSummaryResponse(
                safeLong(projection != null ? projection.getTotalOrders() : null),
                safeLong(projection != null ? projection.getCompletedOrders() : null));
    }

    private TableSummaryResponse toTableSummaryResponse(TableSummaryProjection projection) {
        return new TableSummaryResponse(
                safeLong(projection != null ? projection.getTotalTables() : null),
                safeLong(projection != null ? projection.getOccupiedTables() : null));
    }

    private DateRange normalizeDateRange(LocalDate from, LocalDate to, int defaultDays) {
        LocalDate today = AppTime.now().toLocalDate();

        LocalDate safeTo = to != null ? to : today;
        LocalDate safeFrom = from != null ? from : safeTo.minusDays(defaultDays - 1L);

        if (safeFrom.isAfter(safeTo)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        return new DateRange(safeFrom, safeTo, safeTo.plusDays(1));
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long parseOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(orderId.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Map<String, Object> orderSummaryMap(Long totalOrders, BigDecimal totalRevenue) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("totalRevenue", totalRevenue);
        return result;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private record DateRange(LocalDate from, LocalDate to, LocalDate toExclusive) {}
}
