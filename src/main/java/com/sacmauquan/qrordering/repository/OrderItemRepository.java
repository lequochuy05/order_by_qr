package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.OrderItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

        // Lấy cả Topping/Lựa chọn món
        @EntityGraph(attributePaths = { "menuItem", "orderItemOptions", "orderItemOptions.itemOptionValue" })
        List<OrderItem> findByOrderId(Long orderId);

        // Gợi ý món mua kèm
        @Query(value = """
                        SELECT oi2.menu_item_id
                        FROM order_item oi1
                        JOIN order_item oi2 ON oi1.order_id = oi2.order_id
                        WHERE oi1.menu_item_id = :itemId AND oi2.menu_item_id != :itemId
                        GROUP BY oi2.menu_item_id
                        ORDER BY COUNT(oi2.menu_item_id) DESC
                        LIMIT :limit
                        """, nativeQuery = true)
        List<Long> findTopAssociatedItems(@Param("itemId") Long itemId, @Param("limit") int limit);

        // Món bán chạy
        @Query(value = ""
                        SELECT oi.menu_item_id
                        FROM order_item oi
                        JOIN orders o ON oi.order_id = o.id
                        WHERE o.status = 'COMPLETED'
                        GROUP BY oi.menu_item_id
                        ORDER BY SUM(oi.quantity) DESC
                        LIMIT :limit
                        """, nativeQuery = true)
        List<Long> findTopSellingItemIds(@Param("limit") int limit);

        // Thống kê số lượng đã bán của 1 món
        @Query(value = """
                        SELECT COALESCE(SUM(oi.quantity), 0)
                        FROM order_item oi
                        JOIN orders o ON oi.order_id = o.id
                        WHERE oi.menu_item_id = :itemId AND o.status = 'COMPLETED'
                        """, nativeQuery = true)
        long countTotalSoldByItemId(@Param("itemId") Long itemId);
}
