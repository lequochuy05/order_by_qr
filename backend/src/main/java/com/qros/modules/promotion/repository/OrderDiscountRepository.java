package com.qros.modules.promotion.repository;

import com.qros.modules.promotion.model.OrderDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderDiscountRepository extends JpaRepository<OrderDiscount, Long> {
    Optional<OrderDiscount> findFirstByOrderIdAndCodeSnapshotIgnoreCase(Long orderId, String codeSnapshot);
}
