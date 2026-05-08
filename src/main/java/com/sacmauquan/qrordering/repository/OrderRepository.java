package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.lang.NonNull;
import java.util.*;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // Phân trang là bắt buộc cho bảng Order
    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.orderItemOptions" })
    @NonNull
    Page<Order> findAll(@NonNull Pageable pageable);

    // Tìm kiếm theo trạng thái
    @EntityGraph(attributePaths = { "table", "orderItems" })
    List<Order> findByStatus(Order.OrderStatus status);

    // Tìm đơn hàng đang hoạt động của 1 bàn
    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.orderItemOptions" })
    Optional<Order> findFirstByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId, List<Order.OrderStatus> statuses);

    // Tìm kiếm nâng cao
    @EntityGraph(attributePaths = { "table", "orderItems", "orderItems.menuItem", "orderItems.combo" })
    List<Order> findByStatusIn(List<Order.OrderStatus> statuses);
}
