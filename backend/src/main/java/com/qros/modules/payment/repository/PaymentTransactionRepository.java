package com.qros.modules.payment.repository;

import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.shared.enums.PaymentMethod;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    @EntityGraph(attributePaths = {"order", "order.table"})
    @Query("SELECT t FROM PaymentTransaction t WHERE t.id = :id")
    Optional<PaymentTransaction> findWithOrderById(@Param("id") @NonNull Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"order", "order.table"})
    @Query("SELECT t FROM PaymentTransaction t WHERE t.id = :id")
    Optional<PaymentTransaction> findWithOrderByIdForUpdate(@Param("id") @NonNull Long id);

    Optional<PaymentTransaction> findFirstByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findByExternalReference(String externalReference);

    Optional<PaymentTransaction> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
            Long orderId, PaymentTransactionStatus status);

    Optional<PaymentTransaction> findFirstByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
            Long orderId, PaymentMethod paymentMethod, PaymentTransactionStatus status);

    Optional<PaymentTransaction> findFirstByOrderIdAndPaymentMethodAndStatusInOrderByCreatedAtDesc(
            Long orderId, PaymentMethod paymentMethod, Collection<PaymentTransactionStatus> statuses);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentTransactionStatus status);

    @Query(
            """
                        SELECT t
                        FROM PaymentTransaction t
                        WHERE t.order.id = :orderId
                          AND t.status IN (
                              com.qros.modules.payment.model.enums.PaymentTransactionStatus.CREATING,
                              com.qros.modules.payment.model.enums.PaymentTransactionStatus.PENDING
                          )
                          AND t.paymentMethod <> com.qros.shared.enums.PaymentMethod.CASH
                        """)
    List<PaymentTransaction> findPendingOnlineTransactionsByOrderId(@Param("orderId") Long orderId);

    @Query(
            """
                        SELECT COALESCE(SUM(t.amount), 0)
                        FROM PaymentTransaction t
                        WHERE t.order.id = :orderId
                          AND t.status = :status
                        """)
    BigDecimal sumAmountByOrderIdAndStatus(
            @Param("orderId") Long orderId, @Param("status") PaymentTransactionStatus status);

    default BigDecimal sumPaidAmountByOrderId(Long orderId) {
        return sumAmountByOrderIdAndStatus(orderId, PaymentTransactionStatus.PAID);
    }

    @Query(
            """
                        SELECT t
                        FROM PaymentTransaction t
                        WHERE t.status = :status
                          AND t.expiresAt IS NOT NULL
                          AND t.expiresAt < :now
                        """)
    List<PaymentTransaction> findByStatusAndExpiresAtBefore(
            @Param("status") PaymentTransactionStatus status, @Param("now") LocalDateTime now);

    @Query(
            """
                        SELECT t
                        FROM PaymentTransaction t
                        WHERE t.status IN :statuses
                          AND t.expiresAt IS NOT NULL
                          AND t.expiresAt < :now
                        """)
    List<PaymentTransaction> findByStatusInAndExpiresAtBefore(
            @Param("statuses") Collection<PaymentTransactionStatus> statuses, @Param("now") LocalDateTime now);

    default List<PaymentTransaction> findExpiredPendingTransactions(LocalDateTime now) {
        return findByStatusInAndExpiresAtBefore(
                List.of(PaymentTransactionStatus.CREATING, PaymentTransactionStatus.PENDING), now);
    }
}
