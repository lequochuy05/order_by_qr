package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.Order.OrderStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

/**
 * OrderRepository - Repository interface for managing Order entities.
 * Includes advanced filtering, pagination, and concurrency control logic.
 */
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

        /**
         * Retrieves an order with a specific status for a given table, using a
         * pessimistic write lock.
         * Used to prevent duplicate order creation during simultaneous requests from
         * the same table.
         * 
         * @param tableId Table ID
         * @param status  Target order status
         * @return Optional containing the order if found
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT o FROM Order o WHERE o.table.id = :tableId AND o.status = :status")
        Optional<Order> findFirstByTableIdAndStatusForUpdate(@Param("tableId") Long tableId,
                        @Param("status") OrderStatus status);

        /**
         * Finds orders by their status.
         * 
         * @param status Target status
         * @return List of orders
         */
        @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.menuItem.category",
            "orderItems.combo", "orderItems.orderItemOptions", "orderItems.orderItemOptions.itemOptionValue" })
        List<Order> findByStatus(Order.OrderStatus status);

        /**
         * Finds the most recent order for a table that matches any of the given
         * statuses.
         * 
         * @param tableId  Table ID
         * @param statuses List of target statuses
         * @return Optional containing the most recent order
         */
        @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.menuItem.category",
            "orderItems.combo", "orderItems.orderItemOptions", "orderItems.orderItemOptions.itemOptionValue" })
        Optional<Order> findFirstByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId,
                        List<Order.OrderStatus> statuses);

        /**
         * Finds all orders that have statuses within the provided list.
         * 
         * @param statuses List of target statuses
         * @return List of orders
         */
        @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.menuItem.category",
            "orderItems.combo", "orderItems.orderItemOptions", "orderItems.orderItemOptions.itemOptionValue" })
        List<Order> findByStatusIn(List<Order.OrderStatus> statuses);

        /**
         * Finds the first order for a table with a specific status.
         * 
         * @param tableId Table ID
         * @param status  Target status
         * @return Found Order entity
         */
        Order findFirstByTableIdAndStatus(Long tableId, OrderStatus status);

        /**
         * Retrieves all orders with full details pre-fetched.
         * 
         * @return List of Order entities with complete associations
         */
        @Query("SELECT o FROM Order o")
        @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.combo",
                        "orderItems.orderItemOptions" })
        List<Order> findAllWithDetails();
}
