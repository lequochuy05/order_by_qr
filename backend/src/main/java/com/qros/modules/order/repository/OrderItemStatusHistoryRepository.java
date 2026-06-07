package com.qros.modules.order.repository;

import com.qros.modules.order.model.OrderItemStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemStatusHistoryRepository extends JpaRepository<OrderItemStatusHistory, Long> {
}
