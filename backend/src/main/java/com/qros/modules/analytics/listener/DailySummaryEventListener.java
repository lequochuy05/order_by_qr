package com.qros.modules.analytics.listener;

import com.qros.modules.analytics.service.DailySummaryRefreshService;
import com.qros.shared.event.DomainEvents.OrderSettledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailySummaryEventListener {

    private final DailySummaryRefreshService refreshService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderSettled(OrderSettledEvent event) {
        try {
            refreshService.refreshDate(event.businessDate());
        } catch (RuntimeException exception) {
            log.error(
                    "Could not refresh daily summaries after settling order {} for {}. "
                            + "The reconciliation job will retry.",
                    event.orderId(),
                    event.businessDate(),
                    exception);
        }
    }
}
