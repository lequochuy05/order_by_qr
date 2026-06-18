package com.qros.modules.analytics.controller;

import com.qros.modules.analytics.dto.response.DashboardSummaryResponse;
import com.qros.modules.analytics.dto.response.OrderDetailResponse;
import com.qros.modules.analytics.dto.response.RevenuePointResponse;
import com.qros.modules.analytics.dto.response.SalesTrendPointResponse;
import com.qros.modules.analytics.dto.response.TopSellingItemResponse;
import com.qros.modules.analytics.dto.response.UserPerformanceResponse;
import com.qros.modules.analytics.service.AnalyticsService;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiRoutes.ANALYTICS)
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardSummaryResponse> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(analyticsService.getDashboardSummary(from, to));
    }

    @GetMapping("/revenue")
    public ApiResponse<List<RevenuePointResponse>> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(analyticsService.getRevenueSeries(from, to));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserPerformanceResponse>> users(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10")
                    @Min(value = 1, message = "Limit must be at least 1")
                    @Max(value = 50, message = "Limit cannot exceed 50")
                    int limit) {
        return ApiResponse.success(analyticsService.getUserPerformance(from, to, limit));
    }

    @GetMapping("/orders")
    public ApiResponse<Page<OrderDetailResponse>> orders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(analyticsService.getOrderDetails(from, to, pageable));
    }

    @GetMapping("/top-items")
    public ApiResponse<List<TopSellingItemResponse>> topItems(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10")
                    @Min(value = 1, message = "Limit must be at least 1")
                    @Max(value = 50, message = "Limit cannot exceed 50")
                    int limit) {
        return ApiResponse.success(analyticsService.getTopSellingItems(from, to, limit));
    }

    @GetMapping("/sales-trend")
    public ApiResponse<List<SalesTrendPointResponse>> salesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(analyticsService.getSalesTrend(from, to));
    }

    @GetMapping("/orders/history")
    public ApiResponse<Page<OrderResponse>> getOrderHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String tableNumber,
            @PageableDefault(size = 15, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {
        return ApiResponse.success(analyticsService.getOrderHistory(status, from, to, orderId, tableNumber, pageable));
    }

    @GetMapping("/orders/summary")
    public ApiResponse<Map<String, Object>> getOrderAnalytics(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String tableNumber) {
        return ApiResponse.success(analyticsService.getOrderAnalytics(status, from, to, orderId, tableNumber));
    }
}
