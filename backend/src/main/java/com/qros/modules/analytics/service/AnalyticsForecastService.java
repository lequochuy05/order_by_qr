package com.qros.modules.analytics.service;

import com.qros.modules.analytics.dto.response.PopularItemForecastResponse;
import com.qros.modules.analytics.dto.response.RevenueForecastResponse;
import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import com.qros.modules.analytics.repository.projection.RevenuePointProjection;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsForecastService {

    private static final int FORECAST_HISTORY_DAYS = 30;
    private static final int FORECAST_DAYS = 7;
    private static final int POPULAR_ITEM_FORECAST_LIMIT = 5;

    private final AnalyticsQueryRepository analyticsQueryRepository;

    @Cacheable(value = CacheNames.ANALYTICS, key = "'forecast:revenue'")
    public List<RevenueForecastResponse> getRevenueForecast() {
        LocalDate today = AppTime.now().toLocalDate();
        LocalDate from = today.minusDays(FORECAST_HISTORY_DAYS - 1L);
        LocalDate toExclusive = today.plusDays(1);

        Map<LocalDate, BigDecimal> actualRevenueByDate = getActualRevenueByDate(
                from,
                toExclusive);

        BigDecimal averageRevenue = calculateAverageRevenue(actualRevenueByDate);

        List<RevenueForecastResponse> result = new ArrayList<>();

        for (int i = 0; i < FORECAST_HISTORY_DAYS; i++) {
            LocalDate date = from.plusDays(i);

            result.add(new RevenueForecastResponse(
                    date,
                    actualRevenueByDate.getOrDefault(date, BigDecimal.ZERO),
                    null,
                    false));
        }

        for (int i = 1; i <= FORECAST_DAYS; i++) {
            result.add(new RevenueForecastResponse(
                    today.plusDays(i),
                    null,
                    averageRevenue,
                    true));
        }

        return result;
    }

    @Cacheable(value = CacheNames.ANALYTICS, key = "'forecast:popularItems'")
    public List<PopularItemForecastResponse> getPopularItemsForecast() {
        LocalDate today = AppTime.now().toLocalDate();
        LocalDate from = today.minusDays(FORECAST_HISTORY_DAYS - 1L);
        LocalDate toExclusive = today.plusDays(1);

        return analyticsQueryRepository.topSellingItems(
                from,
                toExclusive,
                POPULAR_ITEM_FORECAST_LIMIT)
                .stream()
                .map(row -> {
                    long quantitySold = safeLong(row.getQuantitySold());

                    long estimatedQuantity = Math.max(
                            1L,
                            Math.round(quantitySold * (double) FORECAST_DAYS / FORECAST_HISTORY_DAYS));

                    return new PopularItemForecastResponse(
                            row.getMenuItemId(),
                            row.getItemName(),
                            row.getCategoryName(),
                            estimatedQuantity);
                })
                .toList();
    }

    private Map<LocalDate, BigDecimal> getActualRevenueByDate(
            LocalDate from,
            LocalDate toExclusive) {
        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();

        List<RevenuePointProjection> rows = analyticsQueryRepository.revenueByDay(
                from,
                toExclusive);

        for (RevenuePointProjection row : rows) {
            revenueByDate.put(
                    LocalDate.parse(row.getDate()),
                    safeMoney(row.getRevenue()));
        }

        return revenueByDate;
    }

    private BigDecimal calculateAverageRevenue(Map<LocalDate, BigDecimal> actualRevenueByDate) {
        BigDecimal totalRevenue = actualRevenueByDate.values()
                .stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalRevenue.divide(
                BigDecimal.valueOf(FORECAST_HISTORY_DAYS),
                2,
                RoundingMode.HALF_UP);
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}
