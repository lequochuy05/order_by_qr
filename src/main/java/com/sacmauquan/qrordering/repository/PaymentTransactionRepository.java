package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.model.PaymentTransaction.TransactionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    @EntityGraph(attributePaths = { "order", "order.table" })
    Optional<PaymentTransaction> findWithOrderById(@NonNull Long id);

    Optional<PaymentTransaction> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, TransactionStatus status);
}
