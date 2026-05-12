package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.OrderItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.*;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

        @EntityGraph(attributePaths = { "menuItem", "orderItemOptions", "orderItemOptions.itemOptionValue" })
        List<OrderItem> findByOrderId(Long orderId);

        @Query(value = """
                        SELECT oi2.menu_item_id
                        FROM order_item oi1
                        JOIN order_item oi2 ON oi1.order_id = oi2.order_id
                        WHERE oi1.menu_item_id = :itemId AND oi2.menu_item_id != :itemId
                        GROUP BY oi2.menu_item_id
                        ORDER BY COUNT(oi2.menu_item_id) DESC
                        """, nativeQuery = true)
        List<Long> findTopAssociatedItems(@Param("itemId") Long itemId, Pageable pageable);

        @Query(value = """
                        SELECT oi.menu_item_id
                        FROM order_item oi
                        JOIN orders o ON oi.order_id = o.id
                        WHERE o.status = 'COMPLETED'
                        GROUP BY oi.menu_item_id
                        ORDER BY SUM(oi.quantity) DESC
                        """, nativeQuery = true)
        List<Long> findTopSellingItemIds(Pageable pageable);

        @Query(value = """
                        SELECT COALESCE(SUM(oi.quantity), 0)
                        FROM order_item oi
                        JOIN orders o ON oi.order_id = o.id
                        WHERE oi.menu_item_id = :itemId AND o.status = 'COMPLETED'
                        """, nativeQuery = true)
        long countTotalSoldByItemId(@Param("itemId") Long itemId);

        @Query(value = """
                        SELECT oi.menu_item_id, COALESCE(SUM(oi.quantity), 0)
                        FROM order_item oi
                        JOIN orders o ON oi.order_id = o.id
                        WHERE oi.menu_item_id IN :ids AND o.status = 'COMPLETED'
                        GROUP BY oi.menu_item_id
                        """, nativeQuery = true)
        List<Object[]> countTotalSoldBatch(@Param("ids") List<Long> ids);
}
