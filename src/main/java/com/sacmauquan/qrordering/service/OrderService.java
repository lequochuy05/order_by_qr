package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.OrderPreviewResponse;
import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {
    List<OrderResponse> getAllOrders();

    Page<OrderResponse> getOrderHistory(String status, LocalDateTime startDate, LocalDateTime endDate, String search,
            @NonNull Pageable pageable);

    Map<String, Object> getOrderStats(String status, LocalDateTime startDate, LocalDateTime endDate);

    OrderResponse updateStatus(@NonNull Long id, @NonNull String status);

    OrderResponse createOrder(@NonNull OrderRequest req);

    void cancelOrderItem(@NonNull Long itemId);

    void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus);

    void markItemPrepared(@NonNull Long itemId);

    List<OrderResponse> getKitchenOrders();

    Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId);

    OrderResponse updateOrderItem(@NonNull Long itemId, int quantity, String notes);

    String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode);

    OrderPreviewResponse preview(@NonNull OrderRequest req);

    List<OrderResponse> getActiveOrders();
}
