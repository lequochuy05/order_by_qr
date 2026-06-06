package com.qros.modules.order.service.impl;

import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.order.dto.OrderPreviewResponse;
import com.qros.modules.order.dto.OrderRequest;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.order.service.OrderCreationService;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderQueryService;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.order.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderCreationService orderCreationService;
    private final OrderStatusService orderStatusService;
    private final OrderQueryService orderQueryService;
    private final OrderPricingService orderPricingService;

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderQueryService.getAllOrders();
    }

    @Override
    public Page<OrderResponse> getOrderHistory(String status, LocalDate startDate,
            LocalDate endDate, String orderId, String tableNumber, @NonNull Pageable pageable) {
        return orderQueryService.getOrderHistory(status, startDate, endDate, orderId, tableNumber, pageable);
    }

    @Override
    public Map<String, Object> getOrderStats(String status, LocalDate startDate, LocalDate endDate, String orderId,
            String tableNumber) {
        return orderQueryService.getOrderStats(status, startDate, endDate, orderId, tableNumber);
    }

    @Override
    public OrderResponse reconcileOrder(@NonNull Long id) {
        return orderQueryService.reconcileOrder(id);
    }

    @Override
    public OrderResponse updateStatus(@NonNull Long id, @NonNull String status) {
        return orderStatusService.updateStatus(id, status);
    }

    @Override
    public OrderResponse createOrder(@NonNull OrderRequest request) {
        return orderCreationService.createOrder(request);
    }

    @Override
    public void cancelOrderItem(@NonNull Long itemId) {
        orderStatusService.cancelOrderItem(itemId);
    }

    @Override
    public Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId) {
        return orderQueryService.getCurrentOrderByTable(tableId);
    }

    @Override
    public Optional<PublicMenuResponse.Order> getPublicCurrentOrderByTable(@NonNull Long tableId) {
        return orderQueryService.getPublicCurrentOrderByTable(tableId);
    }

    @Override
    public OrderResponse updateOrderItem(@NonNull Long itemId, int quantity, String notes) {
        return orderStatusService.updateOrderItem(itemId, quantity, notes);
    }

    @Override
    public String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode) {
        return orderStatusService.payOrder(id, userId, voucherCode);
    }

    @Override
    public OrderPreviewResponse preview(@NonNull OrderRequest request) {
        return orderPricingService.preview(request);
    }

    @Override
    public List<OrderResponse> getActiveOrders() {
        return orderQueryService.getActiveOrders();
    }

    @Override
    public OrderResponse confirmPaid(@NonNull Long id) {
        return orderStatusService.confirmPaid(id);
    }

    @Override
    public OrderResponse cancelOrder(@NonNull Long id) {
        return orderStatusService.cancelOrder(id);
    }

    @Override
    public OrderResponse getOrderById(@NonNull Long id) {
        return orderQueryService.getOrderById(id);
    }

    @Override
    public OrderPreviewResponse getOrderPreviewByTableId(@NonNull Long tableId) {
        return orderQueryService.getOrderPreviewByTableId(tableId);
    }

    @Override
    public void deleteOrder(@NonNull Long id) {
        orderStatusService.deleteOrder(id);
    }
}
