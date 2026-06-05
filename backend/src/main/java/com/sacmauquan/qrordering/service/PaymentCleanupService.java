package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.repository.PaymentTransactionRepository;
import com.sacmauquan.qrordering.util.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PaymentCleanupService - Scheduled reconciliation of stale PayOS transactions.
 * <p>
 * Every 5 minutes, queries PENDING transactions older than 20 minutes and checks
 * their actual status with the PayOS gateway. This prevents orphaned transactions
 * from lingering indefinitely when webhooks are missed or the PayOS gateway
 * doesn't call back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCleanupService {

    private final PaymentTransactionRepository transactionRepository;
    private final PayOS payOS;
    private final PayosService payosService;

    /**
     * Runs every 5 minutes to reconcile stale PENDING PayOS transactions.
     * Transactions older than 20 minutes (the PayOS link expiry) are checked
     * against the remote gateway and their local status is updated accordingly.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    public void reconcileStaleTransactions() {
        LocalDateTime cutoff = AppTime.now().minusMinutes(20);
        List<PaymentTransaction> staleTransactions = transactionRepository.findPendingOlderThan(cutoff);

        if (staleTransactions.isEmpty()) {
            return;
        }

        log.info("[PayOS Cleanup] Found {} stale PENDING transactions older than {}", staleTransactions.size(), cutoff);

        for (PaymentTransaction tx : staleTransactions) {
            try {
                // Call the existing sync method which handles PAID/CANCELLED/EXPIRED
                // via the PayOS gateway check
                payosService.syncPaymentStatus(tx.getId());
            } catch (Exception e) {
                log.error("[PayOS Cleanup] Failed to reconcile transaction {}: {}", tx.getId(), e.getMessage());
            }
        }
    }
}
