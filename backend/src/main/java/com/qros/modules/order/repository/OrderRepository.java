package com.qros.modules.order.repository;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.projection.ActiveOrderSummary;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
        /**
         * Lock active orders of a table when creating/adding order items.
         * Service should take the newest one and handle duplicates if any.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @EntityGraph(attributePaths = {
                        "table",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.combo",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        @Query("""
                        SELECT o FROM Order o
                        WHERE o.table.id = :tableId
                          AND o.status IN :statuses
                        ORDER BY o.createdAt DESC
                        """)
        List<Order> findActiveByTableIdForUpdate(
                        @Param("tableId") Long tableId,
                        @Param("statuses") List<OrderStatus> statuses);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @EntityGraph(attributePaths = {
                        "table",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.combo",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        @Query("""
                        SELECT o FROM Order o
                        WHERE o.tableSession.id = :sessionId
                          AND o.status IN :statuses
                        ORDER BY o.createdAt DESC
                        """)
        List<Order> findActiveByTableSessionIdForUpdate(
                        @Param("sessionId") Long sessionId,
                        @Param("statuses") List<OrderStatus> statuses);

        boolean existsByTableIdAndStatusIn(Long tableId, Collection<OrderStatus> statuses);

        /**
         * Lock one order for payment/cancel/update flow.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @EntityGraph(attributePaths = {
                        "table",
                        "paidBy",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.menuItem.category",
                        "orderItems.combo",
                        "orderItems.combo.items",
                        "orderItems.combo.items.menuItem",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        @Query("SELECT o FROM Order o WHERE o.id = :id")
        Optional<Order> findByIdForUpdate(@Param("id") Long id);

        /**
         * Find latest active/current order of a table by table code.
         */
        @EntityGraph(attributePaths = {
                        "table",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.menuItem.category",
                        "orderItems.combo",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        Optional<Order> findFirstByTable_TableCodeAndStatusInOrderByCreatedAtDesc(
                        String tableCode,
                        Collection<OrderStatus> statuses);

        /**
         * Find latest active/current order of a table.
         */
        @EntityGraph(attributePaths = {
                        "table",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.combo",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        Optional<Order> findFirstByTableIdAndStatusInOrderByCreatedAtDesc(
                        Long tableId,
                        List<OrderStatus> statuses);

        @EntityGraph(attributePaths = {
                        "table",
                        "tableSession",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.combo",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        Optional<Order> findFirstByTableSessionIdAndStatusInOrderByCreatedAtDesc(
                        Long tableSessionId,
                        List<OrderStatus> statuses);

        /**
         * Find orders by multiple statuses.
         */
        @EntityGraph(attributePaths = {
                        "table",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.combo",
                        "orderItems.orderItemOptions"
        })
        List<Order> findByStatusIn(List<OrderStatus> statuses);

        /**
         * Fetch full detail for order detail screen.
         */
        @Query("SELECT o FROM Order o WHERE o.id = :id")
        @EntityGraph(attributePaths = {
                        "table",
                        "paidBy",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.menuItem.category",
                        "orderItems.combo",
                        "orderItems.combo.items",
                        "orderItems.combo.items.menuItem",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        Optional<Order> findDetailById(@Param("id") Long id);

        /**
         * Fetch detail list by ids. Useful after paging/specification query.
         */
        @EntityGraph(attributePaths = {
                        "table",
                        "paidBy",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.menuItem.category",
                        "orderItems.combo",
                        "orderItems.combo.items",
                        "orderItems.combo.items.menuItem",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        List<Order> findDistinctByIdIn(Collection<Long> ids);

        /**
         * Lightweight active order summaries for table board.
         */
        @Query(value = """
                        SELECT o.id AS id,
                               o.status AS status,
                               o.final_amount AS finalAmount,
                               o.table_id AS tableId,
                               t.table_number AS tableNumber,
                               o.created_at AS createdAt
                        FROM orders o
                        JOIN tables t ON t.id = o.table_id AND t.is_deleted = false
                        WHERE o.is_deleted = false
                          AND o.status IN ('PENDING', 'SERVING', 'AWAITING_PAYMENT')
                        ORDER BY o.created_at DESC
                        """, nativeQuery = true)
        List<ActiveOrderSummary> findActiveOrderSummariesForTableBoard();

        /**
         * Orders visible on kitchen board.
         */
        @Query("""
                        SELECT DISTINCT o FROM Order o
                        JOIN o.orderItems oi
                        WHERE o.status IN :statuses
                          AND (
                                oi.status IN :activeItemStatuses
                                OR (oi.status = :finishedStatus AND oi.updatedAt >= :recentlyFinishedCutoff)
                              )
                        ORDER BY o.createdAt ASC
                        """)
        @EntityGraph(attributePaths = {
                        "table",
                        "orderItems",
                        "orderItems.menuItem",
                        "orderItems.menuItem.category",
                        "orderItems.combo",
                        "orderItems.orderItemOptions",
                        "orderItems.orderItemOptions.itemOptionValue"
        })
        List<Order> findKitchenOrders(
                        @Param("statuses") List<OrderStatus> statuses,
                        @Param("activeItemStatuses") List<OrderItemStatus> activeItemStatuses,
                        @Param("finishedStatus") OrderItemStatus finishedStatus,
                        @Param("recentlyFinishedCutoff") LocalDateTime recentlyFinishedCutoff);

}
