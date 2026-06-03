package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.model.PaymentTransaction.TransactionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    @EntityGraph(attributePaths = { "order", "order.table" })
    Optional<PaymentTransaction> findWithOrderById(@NonNull Long id);

    Optional<PaymentTransaction> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId,
            TransactionStatus status);

    Optional<PaymentTransaction> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.order.id = :orderId AND t.status = 'PAID'")
    java.math.BigDecimal sumPaidAmountByOrderId(Long orderId);

    /**
     * Finds PENDING transactions created before the given cutoff that are candidates
     * for remote status reconciliation (e.g., expired/cancelled on PayOS side).
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'PENDING' AND t.createdAt < :cutoff")
    java.util.List<PaymentTransaction> findPendingOlderThan(@org.springframework.lang.NonNull java.time.LocalDateTime cutoff);
}
