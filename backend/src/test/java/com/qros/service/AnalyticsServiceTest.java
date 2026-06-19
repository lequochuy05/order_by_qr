package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import com.qros.modules.analytics.repository.projection.OrderFilterSummaryProjection;
import com.qros.modules.analytics.service.AnalyticsForecastService;
import com.qros.modules.analytics.service.AnalyticsService;
import com.qros.modules.order.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    AnalyticsQueryRepository analyticsQueryRepository;

    @Mock
    AnalyticsForecastService analyticsForecastService;

    @Mock
    OrderService orderService;

    @Mock
    OrderFilterSummaryProjection orderFilterSummary;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsQueryRepository, analyticsForecastService, orderService);
    }

    @Test
    void orderAnalyticsReadsReportingFactSummary() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 19);
        when(analyticsQueryRepository.orderFilterSummary("COMPLETED", from, to.plusDays(1), 42L, "A1"))
                .thenReturn(orderFilterSummary);
        when(orderFilterSummary.getTotalOrders()).thenReturn(3L);
        when(orderFilterSummary.getTotalRevenue()).thenReturn(BigDecimal.valueOf(450_000));

        Map<String, Object> result = analyticsService.getOrderAnalytics(" completed ", from, to, "42", " A1 ");

        assertThat(result).containsEntry("totalOrders", 3L).containsEntry("totalRevenue", BigDecimal.valueOf(450_000));
        verify(analyticsQueryRepository).orderFilterSummary("COMPLETED", from, to.plusDays(1), 42L, "A1");
    }

    @Test
    void invalidOrderIdReturnsEmptySummaryWithoutQueryingOperationalOrders() {
        Map<String, Object> result = analyticsService.getOrderAnalytics(null, null, null, "abc", null);

        assertThat(result).containsEntry("totalOrders", 0L).containsEntry("totalRevenue", BigDecimal.ZERO);
    }
}
