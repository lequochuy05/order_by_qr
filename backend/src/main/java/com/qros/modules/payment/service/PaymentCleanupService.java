package com.qros.modules.payment.service;

import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.shared.time.AppTime;
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

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentService paymentService;

    @Scheduled(fixedRate = 300_000)
    public void reconcileExpiredTransactions() {
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
