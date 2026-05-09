package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.Order.OrderStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // tìm kiếm phân trang và lọc nâng cao
    @Override
    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.orderItemOptions", "orderItems.combo" })
    @NonNull
    Page<Order> findAll(Specification<Order> spec, @NonNull Pageable pageable);

    // chống tạo đơn trùng lặp khi đặt món đồng thời tại 1 bàn
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.table.id = :tableId AND o.status = :status")
    Optional<Order> findFirstByTableIdAndStatusForUpdate(@Param("tableId") Long tableId, @Param("status") OrderStatus status);

    @EntityGraph(attributePaths = { "table", "orderItems" })
    List<Order> findByStatus(Order.OrderStatus status);

    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.orderItemOptions" })
    Optional<Order> findFirstByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId, List<Order.OrderStatus> statuses);

    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.combo" })
    List<Order> findByStatusIn(List<Order.OrderStatus> statuses);
    
    Order findFirstByTableIdAndStatus(Long tableId, OrderStatus status);
    @Query("SELECT o FROM Order o")
    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.combo",
            "orderItems.orderItemOptions" })
    List<Order> findAllWithDetails();
}
