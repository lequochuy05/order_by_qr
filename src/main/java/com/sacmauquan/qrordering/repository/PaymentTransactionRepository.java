package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.PaymentTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.*;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // Tìm giao dịch theo đơn hàng và trạng thái
    List<PaymentTransaction> findByOrderIdAndStatus(Long orderId, PaymentTransaction.TransactionStatus status);

    // Lấy giao dịch mới nhất của đơn hàng
    Optional<PaymentTransaction> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId,
            PaymentTransaction.TransactionStatus status);

    // Tìm giao dịch theo mã
    Optional<PaymentTransaction> findByPayosReference(String payosReference);

    List<PaymentTransaction> findByOrderId(Long orderId);

    // Tính tổng tiền đã thanh toán
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt " +
            "WHERE pt.order.id = :orderId AND pt.status = :status")
    BigDecimal sumAmountByOrderIdAndStatus(@Param("orderId") Long orderId,
            @Param("status") PaymentTransaction.TransactionStatus status);
}
