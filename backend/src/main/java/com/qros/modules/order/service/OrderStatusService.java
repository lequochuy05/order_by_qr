package com.qros.modules.order.service;

import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.analytics.service.ReportingSummaryService;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.promotion.dto.DiscountResult;
import com.qros.modules.promotion.service.OrderDiscountService;
import com.qros.modules.promotion.service.DiscountService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.infrastructure.cache.OrderCacheInvalidationService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.util.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiningTableRepository tableRepository;
    private final UserRepository userRepository;
    private final DiscountService discountService;
    private final OrderStateFactory orderStateFactory;
    private final NotificationService notificationService;
    private final OrderPricingService orderPricingService;
    private final OrderMapper orderMapper;
    private final MeterRegistry meterRegistry;
    private final PaymentTransactionRepository transactionRepository;
    private final OrderAuditService orderAuditService;
    private final OrderDiscountService orderDiscountService;
    private final ReportingSummaryService reportingSummaryService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;

    private Counter ordersCompletedCounter;

    @PostConstruct
    public void initCounters() {
        ordersCompletedCounter = Counter.builder("orders.completed")
                .description("Total number of orders marked as COMPLETED")
                .register(meterRegistry);
    }

    @Transactional
    public OrderResponse updateStatus(@NonNull Long id, @NonNull String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Order.OrderStatus targetStatus;
        try {
            targetStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATUS, "Invalid order status: " + status);
        }

        try {
            orderStateFactory.validateTransition(order.getStatus(), targetStatus);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE, e.getMessage());
        }

        Order.OrderStatus fromStatus = order.getStatus();
        OrderState state = orderStateFactory.getState(targetStatus);
        state.handleRequest(order);
        orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), null, "manual-status-update");

        Order saved = orderRepository.save(Objects.requireNonNull(order));
        recalcTableStatus(order);
        orderCacheInvalidationService.evictAfterOrderMutation(id);
        notificationService.notifyOrderChange();
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public void cancelOrderItem(@NonNull Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        if (item.isPrepared()) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE,
                    "Cannot cancel items that are already prepared");
        }

        Order order = item.getOrder();
        order.getOrderItems().remove(item);
        orderItemRepository.delete(item);

        if (order.getOrderItems().isEmpty()) {
            Order.OrderStatus fromStatus = order.getStatus();
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), null, "last-item-cancelled");
        } else {
            orderPricingService.recalculateOrderTotals(order);
        }

        recalcTableStatus(order);
        orderRepository.save(order);
        orderCacheInvalidationService.evictAfterOrderMutation(order.getId());
        notificationService.notifyOrderChange();
    }

    @Transactional
    public void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus) {
        updateItemStatus(itemId, newStatus, null);
    }

    @Transactional
    public void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus, Long userId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        try {
            OrderItem.OrderItemStatus status = OrderItem.OrderItemStatus.valueOf(newStatus.toUpperCase());
            OrderItem.OrderItemStatus fromStatus = item.getStatus();
            User changedBy = resolveUser(userId);
            Long orderId = item.getOrder().getId();
            item.setStatus(status);
            item.setPrepared(status == OrderItem.OrderItemStatus.FINISHED);
            orderItemRepository.saveAndFlush(item);
            orderAuditService.recordItemStatus(item, fromStatus, status, changedBy, "kitchen-status-update");

            Order order = loadOrderWithItems(orderId, item.getOrder());
            tryAutoPromoteOrder(order, changedBy);
            recalcTableStatus(order);
            orderCacheInvalidationService.evictAfterOrderMutation(orderId);
            notificationService.notifyOrderChange();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_ITEM_STATUS, "Invalid item status code: " + newStatus);
        }
    }

    @Transactional
    public void markItemPrepared(@NonNull Long itemId) {
        updateItemStatus(itemId, "FINISHED");
    }

    @Transactional
    public void markItemPrepared(@NonNull Long itemId, Long userId) {
        updateItemStatus(itemId, "FINISHED", userId);
    }

    @Transactional
    public OrderResponse updateOrderItem(@NonNull Long itemId, int quantity, String notes) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        if (item.isPrepared()) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE,
                    "Cannot update items that have already been prepared");
        }

        item.setQuantity(quantity);
        orderPricingService.recalculateLineTotal(item);
        item.setNotes(notes);
        orderItemRepository.save(item);

        Order order = item.getOrder();
        orderPricingService.recalculateOrderTotals(order);
        orderRepository.save(Objects.requireNonNull(order));

        orderCacheInvalidationService.evictAfterOrderMutation(order.getId());
        notificationService.notifyOrderChange();
        return orderMapper.toResponse(order);
    }

    @Transactional
    public String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "This order is already settled");
        }

        if (order.getStatus() != Order.OrderStatus.AWAITING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_INVALID,
                    "Order must be in AWAITING_PAYMENT status before payment. Current: " + order.getStatus());
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Processing user not found"));

        if (voucherCode != null && !voucherCode.isBlank()) {
            BigDecimal subtotal = currentSubtotalAmount(order);
            DiscountResult result = discountService.applyVoucher(voucherCode, subtotal);
            order.setVoucherCode(voucherCode);
            orderPricingService.setOrderMoney(order, subtotal, result.discountValue());
            orderDiscountService.recordVoucherSnapshot(order, result.voucher(), result.discountValue());
        }

        java.time.LocalDateTime paidAt = AppTime.now();
        BigDecimal finalAmount = currentFinalAmount(order);

        Order.OrderStatus fromStatus = order.getStatus();
        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaymentMethod(Order.PaymentMethod.CASH);
        order.setPaidBy(currentUser);
        order.setPaymentTime(paidAt);
        order.setPaidAmount(finalAmount);
        order.setBusinessDate(order.getPaymentTime().toLocalDate());
        createPaidTransactionIfMissing(order, finalAmount, currentUser, paidAt, "cash:order:" + order.getId());
        orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), currentUser, "cash-payment");
        reportingSummaryService.recordCompletedOrder(order);

        orderRepository.save(Objects.requireNonNull(order));
        recalcTableStatus(order);
        orderCacheInvalidationService.evictAfterOrderMutation(id);
        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        ordersCompletedCounter.increment();
        log.info("Order #{} settled via CASH by Staff Member: {}", id, currentUser.getFullName());

        return "Order settled successfully";
    }

    @Transactional
    public OrderResponse confirmPaid(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            return orderMapper.toResponse(order);
        }

        java.time.LocalDateTime paidAt = AppTime.now();
        BigDecimal finalAmount = currentFinalAmount(order);

        Order.OrderStatus fromStatus = order.getStatus();
        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaymentMethod(Order.PaymentMethod.CASH);
        order.setPaymentTime(paidAt);
        order.setPaidAmount(finalAmount);
        order.setBusinessDate(order.getPaymentTime().toLocalDate());
        createPaidTransactionIfMissing(order, finalAmount, null, paidAt, "manual-confirm:order:" + order.getId());
        orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), null, "manual-confirm-paid");
        reportingSummaryService.recordCompletedOrder(order);

        Order saved = orderRepository.save(order);
        recalcTableStatus(order);
        orderCacheInvalidationService.evictAfterOrderMutation(id);
        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        ordersCompletedCounter.increment();
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Order.OrderStatus fromStatus = order.getStatus();
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), null, "order-cancelled");
        Order saved = orderRepository.save(order);
        recalcTableStatus(order);
        orderCacheInvalidationService.evictAfterOrderMutation(id);
        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public void deleteOrder(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        DiningTable table = order.getTable();
        if (table != null) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        orderRepository.delete(Objects.requireNonNull(order));
        orderCacheInvalidationService.evictAfterOrderMutation(id);
        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
    }

    @Transactional
    public void tryAutoPromoteOrder(Order order) {
        tryAutoPromoteOrder(order, null);
    }

    @Transactional
    public void tryAutoPromoteOrder(Order order, User changedBy) {
        if (order.getStatus() != Order.OrderStatus.PENDING
                && order.getStatus() != Order.OrderStatus.SERVING
                && order.getStatus() != Order.OrderStatus.AWAITING_PAYMENT) {
            return;
        }

        if (order.getOrderItems().isEmpty()) {
            return;
        }

        boolean allDone = order.getOrderItems().stream()
                .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.FINISHED
                        || item.getStatus() == OrderItem.OrderItemStatus.CANCELLED);

        boolean anyCookingOrFinished = order.getOrderItems().stream()
                .anyMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.COOKING
                        || item.getStatus() == OrderItem.OrderItemStatus.FINISHED);

        if (allDone && order.getStatus() != Order.OrderStatus.AWAITING_PAYMENT) {
            Order.OrderStatus fromStatus = order.getStatus();
            order.setStatus(Order.OrderStatus.AWAITING_PAYMENT);
            orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), changedBy, "auto-all-items-done");
            orderRepository.save(order);
            log.info("Order #{} auto-promoted to AWAITING_PAYMENT (all items done)", order.getId());
        } else if (!allDone && order.getStatus() == Order.OrderStatus.AWAITING_PAYMENT) {
            Order.OrderStatus fromStatus = order.getStatus();
            order.setStatus(Order.OrderStatus.SERVING);
            orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), changedBy, "auto-items-reopened");
            orderRepository.save(order);
            log.info("Order #{} auto-demoted to SERVING (items reverted to incomplete)", order.getId());
        } else if (anyCookingOrFinished && order.getStatus() == Order.OrderStatus.PENDING) {
            Order.OrderStatus fromStatus = order.getStatus();
            order.setStatus(Order.OrderStatus.SERVING);
            orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), changedBy,
                    "auto-kitchen-started");
            orderRepository.save(order);
            log.info("Order #{} auto-promoted to SERVING (kitchen started prep)", order.getId());
        }
    }

    public void recalcTableStatus(Order order) {
        DiningTable table = order.getTable();
        if (table == null) {
            return;
        }

        if (order.getOrderItems().isEmpty() || order.getStatus() == Order.OrderStatus.CANCELLED
                || order.getStatus() == Order.OrderStatus.COMPLETED) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
        } else if (order.getStatus() == Order.OrderStatus.AWAITING_PAYMENT) {
            table.setStatus(DiningTable.TableStatus.WAITING_FOR_PAYMENT);
        } else {
            boolean allDone = order.getOrderItems().stream()
                    .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.FINISHED
                            || item.getStatus() == OrderItem.OrderItemStatus.CANCELLED);
            table.setStatus(allDone ? DiningTable.TableStatus.WAITING_FOR_PAYMENT : DiningTable.TableStatus.OCCUPIED);
        }
        tableRepository.save(table);
        notificationService.notifyTableChange();
    }

    private BigDecimal currentFinalAmount(Order order) {
        BigDecimal amount = order.getFinalAmount();
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal currentSubtotalAmount(Order order) {
        return order.getSubtotalAmount() != null ? order.getSubtotalAmount() : BigDecimal.ZERO;
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private Order loadOrderWithItems(Long orderId, Order fallback) {
        return orderRepository.findDistinctByIdIn(java.util.List.of(orderId)).stream()
                .findFirst()
                .orElse(fallback);
    }

    private void createPaidTransactionIfMissing(Order order, BigDecimal amount, User createdBy,
            java.time.LocalDateTime paidAt, String idempotencyKey) {
        if (transactionRepository.findFirstByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }

        PaymentTransaction tx = PaymentTransaction.builder()
                .order(order)
                .amount(amount)
                .status(PaymentTransaction.TransactionStatus.PAID)
                .paymentMethod(PaymentTransaction.PaymentMethod.CASH)
                .createdBy(createdBy)
                .paidAt(paidAt)
                .businessDate(paidAt.toLocalDate())
                .idempotencyKey(idempotencyKey)
                .providerPayload("{\"provider\":\"CASH\",\"event\":\"MANUAL_PAYMENT\",\"reference\":\"\"}")
                .build();
        transactionRepository.save(tx);
    }
}
