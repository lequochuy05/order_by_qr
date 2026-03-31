package com.sacmauquan.qrordering.service;

import org.springframework.lang.NonNull;

import com.sacmauquan.qrordering.dto.OrderPreviewResponse;
import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {
    List<Order> getAllOrders();

    Page<Order> getOrderHistory(String status, LocalDateTime startDate, LocalDateTime endDate, String search,
            Pageable pageable);

    Map<String, Object> getOrderStats(String status, LocalDateTime startDate, LocalDateTime endDate);

    Order updateStatus(@NonNull Long id, @NonNull String status);

    Order createOrder(@NonNull OrderRequest req);

    void cancelOrderItem(@NonNull Long itemId);

    void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus);

    void markItemPrepared(@NonNull Long itemId);

    List<Order> getKitchenOrders();

    Optional<Order> getCurrentOrderByTable(@NonNull Long tableId);

    OrderItem updateOrderItem(@NonNull Long itemId, int quantity, String notes);

    String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode);

    OrderPreviewResponse preview(@NonNull OrderRequest req);
}
