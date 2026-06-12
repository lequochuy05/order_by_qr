package com.qros.modules.order.repository;

import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.repository.projection.ItemSoldSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemAnalyticsRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = """
            SELECT oi2.menu_item_id
            FROM order_item oi1
            JOIN order_item oi2 ON oi1.order_id = oi2.order_id
            JOIN orders o ON o.id = oi1.order_id
            JOIN menu_item mi2 ON mi2.id = oi2.menu_item_id
            WHERE oi1.menu_item_id = :itemId
              AND oi1.menu_item_id IS NOT NULL
              AND oi2.menu_item_id IS NOT NULL
              AND oi2.menu_item_id != :itemId
              AND oi1.status <> 'CANCELLED'
              AND oi2.status <> 'CANCELLED'
              AND oi1.is_deleted = false
              AND oi2.is_deleted = false
              AND o.is_deleted = false
              AND mi2.is_deleted = false
            GROUP BY oi2.menu_item_id
            ORDER BY COUNT(oi2.menu_item_id) DESC
            """, nativeQuery = true)
    List<Long> findTopAssociatedItems(@Param("itemId") Long itemId, Pageable pageable);

    @Query(value = """
            SELECT oi.menu_item_id
            FROM order_item oi
            JOIN orders o ON oi.order_id = o.id
            JOIN menu_item mi ON mi.id = oi.menu_item_id
            WHERE o.status = 'COMPLETED'
              AND oi.status <> 'CANCELLED'
              AND oi.menu_item_id IS NOT NULL
              AND oi.is_deleted = false
              AND o.is_deleted = false
              AND mi.is_deleted = false
            GROUP BY oi.menu_item_id
            ORDER BY SUM(oi.quantity) DESC
            """, nativeQuery = true)
    List<Long> findTopSellingItemIds(Pageable pageable);

    @Query(value = """
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM order_item oi
            JOIN orders o ON oi.order_id = o.id
            JOIN menu_item mi ON mi.id = oi.menu_item_id
            WHERE oi.menu_item_id = :itemId
              AND o.status = 'COMPLETED'
              AND oi.status <> 'CANCELLED'
              AND oi.menu_item_id IS NOT NULL
              AND oi.is_deleted = false
              AND o.is_deleted = false
              AND mi.is_deleted = false
            """, nativeQuery = true)
    long countTotalSoldByItemId(@Param("itemId") Long itemId);

    @Query(value = """
            SELECT oi.menu_item_id AS menuItemId,
                   COALESCE(SUM(oi.quantity), 0) AS totalSold
            FROM order_item oi
            JOIN orders o ON oi.order_id = o.id
            JOIN menu_item mi ON mi.id = oi.menu_item_id
            WHERE oi.menu_item_id IN :ids
              AND o.status = 'COMPLETED'
              AND oi.status <> 'CANCELLED'
              AND oi.menu_item_id IS NOT NULL
              AND oi.is_deleted = false
              AND o.is_deleted = false
              AND mi.is_deleted = false
            GROUP BY oi.menu_item_id
            """, nativeQuery = true)
    List<ItemSoldSummary> countTotalSoldBatch(@Param("ids") List<Long> ids);
}