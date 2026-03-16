package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @EntityGraph(attributePaths = {"table", "paidBy", "orderItems"})
    @NonNull
    List<Order> findAll();

    @EntityGraph(attributePaths = {"table", "paidBy", "orderItems"})
    List<Order> findByTableId(Long tableId);

    @EntityGraph(attributePaths = {"table", "paidBy", "orderItems"})
    List<Order> findByStatus(String status);

    @EntityGraph(attributePaths = {"table"})
    Optional<Order> findFirstByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId, List<String> statuses); // Lấy đơn hàng mới nhất của bàn với trạng thái đã cho
    
    @EntityGraph(attributePaths = {"table", "orderItems"})
    Order findFirstByTableIdAndStatus(Long tableId, String status);
    
}


