package com.qros.modules.promotion.repository;

import com.qros.modules.promotion.model.OrderDiscount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDiscountRepository extends JpaRepository<OrderDiscount, Long> {

    Optional<OrderDiscount> findByOrderIdAndCodeSnapshotIgnoreCase(Long orderId, String codeSnapshot);

    boolean existsByOrderIdAndCodeSnapshotIgnoreCase(Long orderId, String codeSnapshot);

    List<OrderDiscount> findByOrderIdOrderByAppliedAtDesc(Long orderId);
}
