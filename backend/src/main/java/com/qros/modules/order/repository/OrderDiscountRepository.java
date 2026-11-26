package com.qros.modules.order.repository;

import com.qros.modules.order.model.OrderDiscount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDiscountRepository extends JpaRepository<OrderDiscount, Long> {

    Optional<OrderDiscount> findByOrderIdAndCodeSnapshotIgnoreCase(Long orderId, String codeSnapshot);
}
