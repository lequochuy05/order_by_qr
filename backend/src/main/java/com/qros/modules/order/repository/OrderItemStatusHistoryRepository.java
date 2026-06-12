package com.qros.modules.order.repository;

import com.qros.modules.order.model.OrderItemStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemStatusHistoryRepository extends JpaRepository<OrderItemStatusHistory, Long> {

    List<OrderItemStatusHistory> findByOrderItemIdOrderByChangedAtAsc(Long orderItemId);

    List<OrderItemStatusHistory> findByOrderItemOrderIdOrderByChangedAtAsc(Long orderId);
}