package com.qros.modules.subscription.repository;

import com.qros.modules.subscription.model.SubscriptionInvoice;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, Long> {

    Optional<SubscriptionInvoice> findByIdempotencyKey(String idempotencyKey);

    Optional<SubscriptionInvoice> findByGatewayOrderCode(Long gatewayOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.id = :id")
    Optional<SubscriptionInvoice> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.gatewayOrderCode = :orderCode")
    Optional<SubscriptionInvoice> findByGatewayOrderCodeForUpdate(@Param("orderCode") Long orderCode);

    List<SubscriptionInvoice> findTop50ByOrderByCreatedAtDesc();
}
