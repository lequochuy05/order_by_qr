package com.qros.modules.order.repository;

import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.order.model.OrderItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(
            attributePaths = {
                "menuItem",
                "combo",
                "combo.items",
                "combo.items.menuItem",
                "orderItemOptions",
                "orderItemOptions.itemOptionValue"
            })
    List<OrderItem> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :id")
    @EntityGraph(
            attributePaths = {
                "order",
                "menuItem",
                "combo",
                "orderItemOptions",
                "orderItemOptions.itemOptionValue",
                "inventoryReservations",
                "inventoryReservations.inventoryItem"
            })
    Optional<OrderItem> findDetailByIdForUpdate(@Param("id") Long id);

    @Query(
            """
          SELECT oi.menuItem
          FROM OrderItem oi
          JOIN oi.order o
          JOIN oi.menuItem m
          JOIN m.category c
          WHERE oi.menuItem IS NOT NULL
            AND m.active = true
            AND m.available = true
            AND c.active = true
            AND oi.status <> com.qros.modules.order.model.enums.OrderItemStatus.CANCELLED
            AND o.status = com.qros.modules.order.model.enums.OrderStatus.COMPLETED
          GROUP BY oi.menuItem
          ORDER BY SUM(oi.quantity) DESC
          """)
    List<MenuItem> findPopularAvailableMenuItems(Pageable pageable);

    @Query(
            """
          SELECT other.menuItem
          FROM OrderItem base
          JOIN base.order o
          JOIN o.orderItems other
          JOIN other.menuItem m
          JOIN m.category c
          WHERE base.menuItem IS NOT NULL
            AND other.menuItem IS NOT NULL
            AND base.menuItem.id = :itemId
            AND other.menuItem.id <> :itemId
            AND m.active = true
            AND m.available = true
            AND c.active = true
            AND base.status <> com.qros.modules.order.model.enums.OrderItemStatus.CANCELLED
            AND other.status <> com.qros.modules.order.model.enums.OrderItemStatus.CANCELLED
            AND o.status = com.qros.modules.order.model.enums.OrderStatus.COMPLETED
          GROUP BY other.menuItem
          ORDER BY COUNT(other.menuItem.id) DESC
          """)
    List<MenuItem> findCrossSellAvailableMenuItems(@Param("itemId") Long itemId, Pageable pageable);
}
