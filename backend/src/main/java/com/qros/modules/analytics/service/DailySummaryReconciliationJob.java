package com.qros.modules.analytics.service;

import com.qros.modules.analytics.config.AnalyticsProperties;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Lazy(false)
@RequiredArgsConstructor
public class DailySummaryReconciliationJob {

    private final DailySummaryRefreshService refreshService;
    private final AnalyticsProperties analyticsProperties;

    @Scheduled(
            cron = "${analytics.summary-reconcile-cron:0 */15 * * * *}",
            zone = "${analytics.summary-reconcile-zone:Asia/Ho_Chi_Minh}")
    public void reconcileRecentDates() {
        int days = Math.max(1, analyticsProperties.getSummaryReconcileDays());
        for (int offset = 0; offset < days; offset++) {
            var businessDate = AppTime.today().minusDays(offset);
            try {
                refreshService.refreshDate(businessDate);
            } catch (RuntimeException exception) {
                log.error("Could not reconcile daily analytics summary for {}", businessDate, exception);
            }
        }
        log.debug("Reconciled daily analytics summaries for {} day(s)", days);
    }
}
