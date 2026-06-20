package com.qros.modules.order.service;

import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderItemUpdateRequest;
import com.qros.modules.order.dto.request.OrderStatusUpdateRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.PublicOrderResponse;
import com.qros.modules.order.dto.response.TableBoardResponse;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.payment.service.PaymentService;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.idempotency.IdempotencyService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final OrderMapper orderMapper;
    private final IdempotencyService idempotencyService;

    @Transactional
    public OrderResponse createCustomerOrder(@NonNull CustomerCreateOrderRequest request) {
        String requestKey = request.tableCode() + ":" + request.sessionToken() + ":" + request.clientRequestId();
        return idempotencyService.execute(
                "public-order",
                requestKey,
                request,
                OrderResponse.class,
                () -> orderCreationService.createCustomerOrder(request));
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

    public Map<String, Object> getOrderAnalytics(
            String status, LocalDate from, LocalDate to, String orderId, String tableNumber) {
        return orderQueryService.getOrderAnalytics(status, from, to, orderId, tableNumber);
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

    public Optional<PublicOrderResponse> getPublicCurrentOrderBySession(
            @NonNull String tableCode, @NonNull String sessionToken) {
        return orderQueryService.getPublicCurrentOrderBySession(tableCode, sessionToken);
    }

    public Optional<PublicOrderResponse> getPublicCurrentOrderByTable(@NonNull Long tableId) {
        return orderQueryService.getPublicCurrentOrderByTable(tableId);
    }

    public OrderPreviewResponse getOrderPreviewByTableId(@NonNull Long tableId) {
        return orderQueryService.getOrderPreviewByTableId(tableId);
    }

    @Transactional
    public OrderResponse reconcileOrder(@NonNull Long id) {
        Order order =
                orderRepository.findDetailById(id).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            return orderMapper.toResponse(order);
        }

        Optional<PaymentTransaction> latestTx =
                transactionRepository.findFirstByOrderIdAndPaymentMethodAndStatusInOrderByCreatedAtDesc(
                        order.getId(),
                        PaymentMethod.PAYOS,
                        List.of(PaymentTransactionStatus.CREATING, PaymentTransactionStatus.PENDING));

        if (latestTx.isPresent()) {
            paymentService.syncPaymentStatus(latestTx.get().getId());

            order = orderRepository.findDetailById(id).orElse(order);
        }

        return orderMapper.toResponse(order);
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

    public void updateItemStatus(@NonNull Long itemId, @NonNull OrderItemStatus status, Long userId) {
        orderItemWorkflowService.updateItemStatus(itemId, status, userId);
    }

    public void markItemPrepared(@NonNull Long itemId, Long userId) {
        orderItemWorkflowService.markItemPrepared(itemId, userId);
    }
}
