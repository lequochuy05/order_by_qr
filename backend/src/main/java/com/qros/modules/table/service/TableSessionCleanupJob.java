package com.qros.modules.table.service;

import com.qros.shared.scheduling.DistributedSchedulerLock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled cleanup trigger that acquires a distributed lock before invoking
 * {@link TableSessionService#expireStaleOpenSessionsWithoutOrders()} so non-owner replicas
 * never open a database transaction.
 */
@Component
@Lazy(false)
@RequiredArgsConstructor
public class TableSessionCleanupJob {

    private static final String EXPIRE_LOCK_NAME = "table-session-expire";
    private static final Duration EXPIRE_LOCK_TTL = Duration.ofMinutes(4);

    private final TableSessionService tableSessionService;
    private final DistributedSchedulerLock schedulerLock;

    @Scheduled(fixedDelayString = "${table-session.cleanup-fixed-delay-ms:300000}")
    public void expireStaleOpenSessionsWithoutOrders() {
        schedulerLock.runWithLock(
                EXPIRE_LOCK_NAME, EXPIRE_LOCK_TTL, tableSessionService::expireStaleOpenSessionsWithoutOrders);
    }
}
