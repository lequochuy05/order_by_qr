package com.qros.modules.order.repository;

import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.order.model.OrderItem;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :id")
    @EntityGraph(
            attributePaths = {"order", "menuItem", "combo", "orderItemOptions", "orderItemOptions.itemOptionValue"})
    Optional<OrderItem> findDetailByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT COUNT(oi) > 0
            FROM OrderItem oi
            JOIN oi.order o
            WHERE oi.menuItem.id = :menuItemId
              AND o.status IN (
                  com.qros.modules.order.model.enums.OrderStatus.PENDING,
                  com.qros.modules.order.model.enums.OrderStatus.SERVING,
                  com.qros.modules.order.model.enums.OrderStatus.AWAITING_PAYMENT
              )
            """)
    boolean existsByMenuItemIdInActiveOrders(@Param("menuItemId") Long menuItemId);

    @Query("""
            SELECT COUNT(oi) > 0
            FROM OrderItem oi
            JOIN oi.order o
            WHERE oi.combo.id = :comboId
              AND o.status IN (
                  com.qros.modules.order.model.enums.OrderStatus.PENDING,
                  com.qros.modules.order.model.enums.OrderStatus.SERVING,
                  com.qros.modules.order.model.enums.OrderStatus.AWAITING_PAYMENT
              )
            """)
    boolean existsByComboIdInActiveOrders(@Param("comboId") Long comboId);

    @Query("""
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

    @Query("""
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

    @Query("""
          SELECT m1.id,
                 m1.name,
                 m2.id,
                 m2.name,
                 COUNT(DISTINCT o.id)
          FROM OrderItem firstItem
          JOIN firstItem.order o
          JOIN o.orderItems secondItem
          JOIN firstItem.menuItem m1
          JOIN m1.category c1
          JOIN secondItem.menuItem m2
          JOIN m2.category c2
          WHERE m1.id < m2.id
            AND m1.active = true
            AND m1.available = true
            AND c1.active = true
            AND m2.active = true
            AND m2.available = true
            AND c2.active = true
            AND firstItem.status <> com.qros.modules.order.model.enums.OrderItemStatus.CANCELLED
            AND secondItem.status <> com.qros.modules.order.model.enums.OrderItemStatus.CANCELLED
            AND o.status = com.qros.modules.order.model.enums.OrderStatus.COMPLETED
            AND o.createdAt >= :from
          GROUP BY m1.id, m1.name, m2.id, m2.name
          ORDER BY COUNT(DISTINCT o.id) DESC
          """)
    List<Object[]> findFrequentlyPairedMenuItems(@Param("from") LocalDateTime from, Pageable pageable);
}
