package com.qros.modules.order.service;

import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderItemUpdateRequest;
import com.qros.modules.order.dto.request.OrderPayRequest;
import com.qros.modules.order.dto.request.OrderStatusUpdateRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.PublicOrderResponse;
import com.qros.modules.order.dto.response.TableBoardResponse;
import com.qros.modules.order.model.enums.OrderItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OrderService - Facade service for the order module.
 * Controllers should call this class instead of accessing smaller order
 * services directly.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

        private final OrderCreationService orderCreationService;
        private final OrderQueryService orderQueryService;
        private final OrderPricingService orderPricingService;
        private final OrderStatusService orderStatusService;
        private final OrderItemWorkflowService orderItemWorkflowService;
        private final OrderPaymentService orderPaymentService;

        public OrderResponse createCustomerOrder(@NonNull CustomerCreateOrderRequest request) {
                return orderCreationService.createCustomerOrder(request);
        }

        public OrderResponse createStaffOrder(@NonNull StaffCreateOrderRequest request) {
                return orderCreationService.createStaffOrder(request);
        }

        public OrderPreviewResponse previewCustomerOrder(@NonNull CustomerCreateOrderRequest request) {
                return orderPricingService.previewCustomerOrder(request);
        }

        public OrderPreviewResponse previewStaffOrder(@NonNull StaffCreateOrderRequest request) {
                return orderPricingService.previewStaffOrder(request);
        }

        public Page<OrderResponse> getAllOrders(@NonNull Pageable pageable) {
                return orderQueryService.getAllOrders(pageable);
        }

        public Page<OrderResponse> getOrderHistory(
                        String status,
                        LocalDate from,
                        LocalDate to,
                        String orderId,
                        String tableNumber,
                        @NonNull Pageable pageable) {
                return orderQueryService.getOrderHistory(status, from, to, orderId, tableNumber, pageable);
        }

        public Map<String, Object> getOrderStats(
                        String status,
                        LocalDate from,
                        LocalDate to,
                        String orderId,
                        String tableNumber) {
                return orderQueryService.getOrderStats(status, from, to, orderId, tableNumber);
        }

        public List<OrderResponse> getActiveOrders() {
                return orderQueryService.getActiveOrders();
        }

        public TableBoardResponse getTableBoard() {
                return orderQueryService.getTableBoard();
        }

        public OrderResponse getOrderById(@NonNull Long id) {
                return orderQueryService.getOrderById(id);
        }

        public Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId) {
                return orderQueryService.getCurrentOrderByTable(tableId);
        }

        public Optional<PublicOrderResponse> getPublicCurrentOrderByTableCode(@NonNull String tableCode) {
                return orderQueryService.getPublicCurrentOrderByTableCode(tableCode);
        }

        public Optional<PublicOrderResponse> getPublicCurrentOrderByTable(@NonNull Long tableId) {
                return orderQueryService.getPublicCurrentOrderByTable(tableId);
        }

        public OrderPreviewResponse getOrderPreviewByTableId(@NonNull Long tableId) {
                return orderQueryService.getOrderPreviewByTableId(tableId);
        }

        public OrderResponse reconcileOrder(@NonNull Long id) {
                return orderQueryService.reconcileOrder(id);
        }

        public OrderResponse updateStatus(@NonNull Long id, @NonNull OrderStatusUpdateRequest request) {
                return orderStatusService.updateStatus(id, request.status());
        }

        public OrderResponse cancelOrder(@NonNull Long id) {
                return orderStatusService.cancelOrder(id);
        }

        public void deleteOrder(@NonNull Long id) {
                orderStatusService.deleteOrder(id);
        }

        public OrderResponse updateOrderItem(@NonNull Long itemId, @NonNull OrderItemUpdateRequest request) {
                return orderItemWorkflowService.updateOrderItem(itemId, request.quantity(), request.notes());
        }

        public void cancelOrderItem(@NonNull Long itemId) {
                orderItemWorkflowService.cancelOrderItem(itemId);
        }

        public void updateItemStatus(@NonNull Long itemId, @NonNull OrderItemStatus status) {
                orderItemWorkflowService.updateItemStatus(itemId, status);
        }

        public void updateItemStatus(@NonNull Long itemId, @NonNull OrderItemStatus status, Long userId) {
                orderItemWorkflowService.updateItemStatus(itemId, status, userId);
        }

        public void markItemPrepared(@NonNull Long itemId) {
                orderItemWorkflowService.markItemPrepared(itemId);
        }

        public void markItemPrepared(@NonNull Long itemId, Long userId) {
                orderItemWorkflowService.markItemPrepared(itemId, userId);
        }

        public String payOrder(@NonNull Long id, @NonNull OrderPayRequest request) {
                return orderPaymentService.payOrder(id, request.voucherCode());
        }

        public String payOrder(@NonNull Long id, Long userId, @NonNull OrderPayRequest request) {
                return orderPaymentService.payOrder(id, userId, request.voucherCode());
        }

        public OrderResponse confirmPaid(@NonNull Long id) {
                return orderPaymentService.confirmPaid(id);
        }
}