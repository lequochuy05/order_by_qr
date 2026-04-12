package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    
    @EntityGraph(attributePaths = { "table", "paidBy", "orderItems" })
    @NonNull
    List<Order> findAll();

    @EntityGraph(attributePaths = { "table", "paidBy", "orderItems" })
    List<Order> findByTableId(Long tableId);

    @EntityGraph(attributePaths = { "table", "paidBy", "orderItems" })
    List<Order> findByStatus(String status);

    @EntityGraph(attributePaths = { "table" })
    Optional<Order> findFirstByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId, List<String> statuses);
    
    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.combo" })
    Order findFirstByTableIdAndStatus(Long tableId, String status);

    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.combo" })
    List<Order> findByStatusIn(List<String> statuses);
}
