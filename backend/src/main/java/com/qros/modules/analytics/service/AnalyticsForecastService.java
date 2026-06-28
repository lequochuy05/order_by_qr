package com.qros.modules.analytics.service;

import com.qros.modules.analytics.dto.response.PopularItemForecastResponse;
import com.qros.modules.analytics.dto.response.RevenueForecastResponse;
import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import com.qros.modules.analytics.repository.projection.RevenuePointProjection;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsForecastService {

    private static final int FORECAST_HISTORY_DAYS = 30;
    private static final int FORECAST_DAYS = 7;
    private static final int WEEKLY_SEASON_LENGTH = 7;
    private static final int POPULAR_ITEM_FORECAST_LIMIT = 5;

    // Holt-Winters Constants
    private static final double ALPHA = 0.30;
    private static final double BETA = 0.10;
    private static final double GAMMA = 0.40;
    private static final int MIN_HISTORY_DAYS_FOR_HOLT_WINTERS = 21;
    private static final int MIN_NON_ZERO_DAYS_FOR_HOLT_WINTERS = 7;

    private final AnalyticsQueryRepository analyticsQueryRepository;

    private record DailyRevenuePoint(LocalDate date, double revenue) {}

    private record HoltWintersState(double level, double trend, double[] seasonality) {}

    @Cacheable(value = CacheNames.ANALYTICS, key = "'forecast:revenue'")
    public List<RevenueForecastResponse> getRevenueForecast() {
        LocalDate today = AppTime.now().toLocalDate();
        // Lịch sử: 30 ngày, kết thúc ở hôm qua (không bao gồm hôm nay)
        LocalDate from = today.minusDays(FORECAST_HISTORY_DAYS);
        LocalDate toExclusive = today;

        Map<LocalDate, BigDecimal> actualRevenueMap = getActualRevenueByDate(from, toExclusive);
        List<DailyRevenuePoint> series = buildContinuousRevenueSeries(actualRevenueMap, today.minusDays(1), FORECAST_HISTORY_DAYS);

        List<RevenueForecastResponse> result = new ArrayList<>();

        // Add history points (đã xong, không bao gồm hôm nay)
        for (DailyRevenuePoint point : series) {
            result.add(new RevenueForecastResponse(point.date(), BigDecimal.valueOf(point.revenue()), null, false));
        }

        // Check if we can use Holt-Winters
        long nonZeroDays = series.stream().filter(p -> p.revenue() > 0).count();
        boolean useHoltWinters =
                series.size() >= MIN_HISTORY_DAYS_FOR_HOLT_WINTERS && nonZeroDays >= MIN_NON_ZERO_DAYS_FOR_HOLT_WINTERS;

        // Dự báo 7 ngày, bao gồm hôm nay (để forecast cũ không bị mất)
        if (useHoltWinters) {
            HoltWintersState state = trainHoltWinters(series);
            for (int m = 0; m < FORECAST_DAYS; m++) {
                LocalDate forecastDate = today.plusDays(m);
                int index = forecastDate.getDayOfWeek().getValue() - 1;

                double rawForecast = state.level() + state.trend() * (m + 1) + state.seasonality()[index];
                double clampedForecast = Math.max(0, rawForecast);

                BigDecimal forecastRevenue = BigDecimal.valueOf(clampedForecast).setScale(0, RoundingMode.HALF_UP);
                result.add(new RevenueForecastResponse(forecastDate, null, forecastRevenue, true));
            }
        } else {
            // Fallback to Simple Moving Average
            BigDecimal averageRevenue = calculateAverageRevenue(actualRevenueMap);
            for (int m = 0; m < FORECAST_DAYS; m++) {
                result.add(new RevenueForecastResponse(today.plusDays(m), null, averageRevenue, true));
            }
        }

        return result;
    }

    @Cacheable(value = CacheNames.ANALYTICS, key = "'forecast:popularItems'")
    public List<PopularItemForecastResponse> getPopularItemsForecast() {
        LocalDate today = AppTime.now().toLocalDate();
        LocalDate from = today.minusDays(FORECAST_HISTORY_DAYS - 1L);
        LocalDate toExclusive = today.plusDays(1);

        return analyticsQueryRepository.topSellingItems(from, toExclusive, POPULAR_ITEM_FORECAST_LIMIT).stream()
                .map(row -> {
                    long quantitySold = safeLong(row.getQuantitySold());

                    // Thêm 5% buffer an toàn cho việc gợi ý chuẩn bị món
                    double bufferedQuantity = quantitySold * 1.05;
                    long estimatedQuantity =
                            Math.max(1L, Math.round(bufferedQuantity * FORECAST_DAYS / FORECAST_HISTORY_DAYS));

                    return new PopularItemForecastResponse(
                            row.getMenuItemId(), row.getItemName(), row.getCategoryName(), estimatedQuantity);
                })
                .toList();
    }

    private List<DailyRevenuePoint> buildContinuousRevenueSeries(
            Map<LocalDate, BigDecimal> actualRevenue, LocalDate endDate, int historyDays) {
        List<DailyRevenuePoint> series = new ArrayList<>();
        LocalDate startDate = endDate.minusDays(historyDays - 1L);

        for (int i = 0; i < historyDays; i++) {
            LocalDate date = startDate.plusDays(i);
            BigDecimal revenue = actualRevenue.getOrDefault(date, BigDecimal.ZERO);
            series.add(new DailyRevenuePoint(date, revenue.doubleValue()));
        }

        return series;
    }

    private HoltWintersState trainHoltWinters(List<DailyRevenuePoint> series) {
        double level = initialLevel(series);
        double trend = initialTrend(series);
        double[] seasonality = initialSeasonality(series);

        for (DailyRevenuePoint point : series) {
            double value = point.revenue();
            int index = point.date().getDayOfWeek().getValue() - 1;

            double seasonal = seasonality[index];
            double previousLevel = level;

            level = ALPHA * (value - seasonal) + (1 - ALPHA) * (level + trend);
            trend = BETA * (level - previousLevel) + (1 - BETA) * trend;
            seasonality[index] = GAMMA * (value - level) + (1 - GAMMA) * seasonal;
        }

        return new HoltWintersState(level, trend, seasonality);
    }

    private double initialLevel(List<DailyRevenuePoint> series) {
        return series.subList(0, WEEKLY_SEASON_LENGTH).stream()
                .mapToDouble(DailyRevenuePoint::revenue)
                .average()
                .orElse(0.0);
    }

    private double initialTrend(List<DailyRevenuePoint> series) {
        if (series.size() < WEEKLY_SEASON_LENGTH * 2) return 0.0;

        double firstWeekAvg = initialLevel(series);
        double secondWeekAvg = series.subList(WEEKLY_SEASON_LENGTH, WEEKLY_SEASON_LENGTH * 2).stream()
                .mapToDouble(DailyRevenuePoint::revenue)
                .average()
                .orElse(0.0);

        return (secondWeekAvg - firstWeekAvg) / WEEKLY_SEASON_LENGTH;
    }

    private double[] initialSeasonality(List<DailyRevenuePoint> series) {
        double globalAverage = series.stream()
                .mapToDouble(DailyRevenuePoint::revenue)
                .average()
                .orElse(0.0);

        double[] seasonality = new double[7];
        int[] counts = new int[7];

        for (DailyRevenuePoint point : series) {
            int index = point.date().getDayOfWeek().getValue() - 1;
            seasonality[index] += point.revenue() - globalAverage;
            counts[index]++;
        }

        for (int i = 0; i < 7; i++) {
            seasonality[i] = counts[i] > 0 ? seasonality[i] / counts[i] : 0;
        }

        return seasonality;
    }

    private Map<LocalDate, BigDecimal> getActualRevenueByDate(LocalDate from, LocalDate toExclusive) {
        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        List<RevenuePointProjection> rows = analyticsQueryRepository.revenueByDay(from, toExclusive);

        for (RevenuePointProjection row : rows) {
            revenueByDate.put(LocalDate.parse(row.getDate()), safeMoney(row.getRevenue()));
        }

        return revenueByDate;
    }

    private BigDecimal calculateAverageRevenue(Map<LocalDate, BigDecimal> actualRevenueByDate) {
        BigDecimal totalRevenue = actualRevenueByDate.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalRevenue.divide(BigDecimal.valueOf(FORECAST_HISTORY_DAYS), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}
