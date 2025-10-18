package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTableId(Long tableId);
    List<Order> findByStatus(String status);
    Optional<Order> findFirstByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId, List<String> statuses); // Lấy đơn hàng mới nhất của bàn với trạng thái đã cho
    Order findFirstByTableIdAndStatus(Long tableId, String status);
    
}


