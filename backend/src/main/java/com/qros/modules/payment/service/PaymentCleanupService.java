package com.qros.modules.payment.service;

import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.shared.scheduling.DistributedSchedulerLock;
import com.qros.shared.time.AppTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Lazy(false)
@RequiredArgsConstructor
public class PaymentCleanupService {

    private static final String LOCK_NAME = "payment-cleanup-reconcile";
    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final DistributedSchedulerLock schedulerLock;

    @Scheduled(fixedRate = 300_000)
    public void reconcileExpiredTransactions() {
        schedulerLock.runWithLock(LOCK_NAME, LOCK_TTL, this::doReconcileExpiredTransactions);
    }

    private void doReconcileExpiredTransactions() {
        LocalDateTime now = AppTime.now();
        List<PaymentTransaction> expiredTransactions = transactionRepository.findExpiredPendingTransactions(now);

        if (expiredTransactions.isEmpty()) {
            return;
        }

        log.info("[Payment Cleanup] Found {} expired pending transactions before {}", expiredTransactions.size(), now);

        for (PaymentTransaction transaction : expiredTransactions) {
            try {
                paymentService.syncPaymentStatus(transaction.getId());
            } catch (Exception e) {
                log.error(
                        "[Payment Cleanup] Failed to reconcile transaction {}: {}",
                        transaction.getId(),
                        e.getMessage());
            }
        }
    }
}
