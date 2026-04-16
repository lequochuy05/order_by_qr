package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @EntityGraph(attributePaths = { "menuItem" })
    List<OrderItem> findByOrderId(Long orderId);

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
}
