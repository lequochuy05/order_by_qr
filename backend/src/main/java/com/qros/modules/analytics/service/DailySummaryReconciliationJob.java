package com.qros.modules.analytics.service;

import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailySummaryReconciliationJob {

    private final DailySummaryRefreshService refreshService;

    @Value("${analytics.summary-reconcile-days:2}")
    private int reconcileDays;

    @Scheduled(
            cron = "${analytics.summary-reconcile-cron:0 */15 * * * *}",
            zone = "${analytics.summary-reconcile-zone:Asia/Ho_Chi_Minh}")
    public void reconcileRecentDates() {
        int days = Math.max(1, reconcileDays);
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
