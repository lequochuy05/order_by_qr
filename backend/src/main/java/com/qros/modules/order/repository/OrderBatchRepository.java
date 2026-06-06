package com.qros.modules.order.repository;

import com.qros.modules.order.model.OrderBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderBatchRepository extends JpaRepository<OrderBatch, Long> {
}
