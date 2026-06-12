package com.qros.modules.order.repository;

import com.qros.modules.order.model.OrderBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderBatchRepository extends JpaRepository<OrderBatch, Long> {

    List<OrderBatch> findByOrderIdOrderBySubmittedAtAsc(Long orderId);

    Optional<OrderBatch> findFirstByOrderIdOrderBySubmittedAtDesc(Long orderId);
}