package com.qros.modules.order.service;

import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.listener.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.service.TableSessionService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderStateFactory orderStateFactory;
    private final OrderAuditService orderAuditService;
    private final OrderTableSyncService orderTableSyncService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final OrderInventoryReservationPort inventoryReservationService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderMapper orderMapper;
    private final TableSessionService tableSessionService;
    private final UserLookupService userRepository;
    private final OrderPaymentPort orderPaymentPort;

    public OrderStatusService(
            OrderRepository orderRepository,
            OrderStateFactory orderStateFactory,
            OrderAuditService orderAuditService,
            OrderTableSyncService orderTableSyncService,
            OrderCacheInvalidationService orderCacheInvalidationService,
            OrderInventoryReservationPort inventoryReservationService,
            ApplicationEventPublisher eventPublisher,
            OrderMapper orderMapper,
            TableSessionService tableSessionService) {
        this(
                orderRepository,
                orderStateFactory,
                orderAuditService,
                orderTableSyncService,
                orderCacheInvalidationService,
                inventoryReservationService,
                eventPublisher,
                orderMapper,
                tableSessionService,
                null,
                null);
    }

    public OrderStatusService(
            OrderRepository orderRepository,
            OrderStateFactory orderStateFactory,
            OrderAuditService orderAuditService,
            OrderTableSyncService orderTableSyncService,
            OrderCacheInvalidationService orderCacheInvalidationService,
            OrderInventoryReservationPort inventoryReservationService,
            ApplicationEventPublisher eventPublisher,
            OrderMapper orderMapper,
            TableSessionService tableSessionService,
            UserLookupService userRepository) {
        this(
                orderRepository,
                orderStateFactory,
                orderAuditService,
                orderTableSyncService,
                orderCacheInvalidationService,
                inventoryReservationService,
                eventPublisher,
                orderMapper,
                tableSessionService,
                userRepository,
                null);
    }

    @Autowired
    public OrderStatusService(
            OrderRepository orderRepository,
            OrderStateFactory orderStateFactory,
            OrderAuditService orderAuditService,
            OrderTableSyncService orderTableSyncService,
            OrderCacheInvalidationService orderCacheInvalidationService,
            OrderInventoryReservationPort inventoryReservationService,
            ApplicationEventPublisher eventPublisher,
            OrderMapper orderMapper,
            TableSessionService tableSessionService,
            UserLookupService userRepository,
            OrderPaymentPort orderPaymentPort) {
        this.orderRepository = orderRepository;
        this.orderStateFactory = orderStateFactory;
        this.orderAuditService = orderAuditService;
        this.orderTableSyncService = orderTableSyncService;
        this.orderCacheInvalidationService = orderCacheInvalidationService;
        this.inventoryReservationService = inventoryReservationService;
        this.eventPublisher = eventPublisher;
        this.orderMapper = orderMapper;
        this.tableSessionService = tableSessionService;
        this.userRepository = userRepository;
        this.orderPaymentPort = orderPaymentPort;
    }

    private User resolveUserByEmail(String email) {
        if (userRepository == null || email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
    }

    @Transactional
    public OrderResponse updateStatus(@NonNull Long id, @NonNull OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_INVALID, "Use payment endpoint to complete an order");
        }

        if (targetStatus == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE, "Use cancel endpoint to cancel an order");
        }

        Order order = orderRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == targetStatus) {
            return orderMapper.toResponse(order);
        }

        try {
            orderStateFactory.validateTransition(order.getStatus(), targetStatus);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE, e.getMessage());
        }

        OrderStatus fromStatus = order.getStatus();

        OrderState state = orderStateFactory.getState(targetStatus);
        try {
            state.handleRequest(order);
        } catch (IllegalStateException e) {
            throw new BusinessException(
                    targetStatus == OrderStatus.AWAITING_PAYMENT
                            ? ErrorCode.ORDER_HAS_UNFINISHED_ITEMS
                            : ErrorCode.ORDER_INVALID_STATE,
                    e.getMessage());
        }

        orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), null, "manual-status-update");

        Order saved = orderRepository.save(order);

        orderTableSyncService.recalcTableStatus(saved);
        orderCacheInvalidationService.evictAfterOrderMutation(saved);
        eventPublisher.publishEvent(new OrderChangeEvent());

        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(@NonNull Long id) {
        return cancelOrder(id, null, null);
    }

    @Transactional
    public OrderResponse cancelOrder(@NonNull Long id, String actorEmail, String reason) {
        Order order = orderRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.canBeCancelled()) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE, "Cannot cancel this order in current state");
        }

        ensureNoPendingPayment(order, "Cannot cancel an order while an online payment is in progress");

        List<String> tableCodes = orderCacheInvalidationService.resolveTableCodes(order);

        OrderStatus fromStatus = order.getStatus();
        User changedBy = resolveUserByEmail(actorEmail);
        String auditReason = (reason != null && !reason.isBlank()) ? reason.trim() : "order-cancelled";

        order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.FINISHED)
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .forEach(item -> {
                    OrderItemStatus fromItemStatus = item.getStatus();

                    inventoryReservationService.restoreOrderItem(item);
                    item.markStatus(OrderItemStatus.CANCELLED);

                    orderAuditService.recordItemStatus(
                            item, fromItemStatus, OrderItemStatus.CANCELLED, changedBy, auditReason);
                });

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);

        orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), changedBy, auditReason);

        Order saved = orderRepository.save(order);

        orderTableSyncService.recalcTableStatus(saved);
        if (saved.getTableSession() != null) {
            tableSessionService.closeSession(
                    saved.getTableSession().getId(), TableSessionStatus.CANCELLED, auditReason);
        }
        orderCacheInvalidationService.evictAfterOrderMutation(saved, tableCodes);
        eventPublisher.publishEvent(new OrderChangeEvent());

        return orderMapper.toResponse(saved);
    }

    @Transactional
    public void deleteOrder(@NonNull Long id) {
        Order order = orderRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.isPaid() || order.isCompleted()) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "Cannot delete a paid or completed order");
        }

        ensureNoPendingPayment(order, "Cannot delete an order while an online payment is in progress");

        List<String> tableCodes = orderCacheInvalidationService.resolveTableCodes(order);

        restoreReservableItems(order);

        orderTableSyncService.releaseTable(order);
        if (order.getTableSession() != null) {
            tableSessionService.closeSession(
                    order.getTableSession().getId(), TableSessionStatus.CANCELLED, "Order deleted");
        }

        orderRepository.delete(order);

        orderCacheInvalidationService.evictAfterOrderMutation(order, tableCodes);
        eventPublisher.publishEvent(new OrderChangeEvent());
    }

    @Transactional
    public void tryAutoPromoteOrder(Order order) {
        tryAutoPromoteOrder(order, null);
    }

    @Transactional
    public void tryAutoPromoteOrder(Order order, User changedBy) {
        if (!List.of(OrderStatus.PENDING, OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT)
                .contains(order.getStatus())) {
            return;
        }

        List<OrderItem> billableItems =
                order.getOrderItems().stream().filter(OrderItem::isBillable).toList();

        if (billableItems.isEmpty()) {
            return;
        }

        boolean allDone = billableItems.stream().allMatch(item -> item.getStatus() == OrderItemStatus.FINISHED);

        boolean anyCookingOrFinished = billableItems.stream()
                .anyMatch(item ->
                        item.getStatus() == OrderItemStatus.COOKING || item.getStatus() == OrderItemStatus.FINISHED);

        if (allDone && order.isPaid()) {
            List<String> tableCodes = orderCacheInvalidationService.resolveTableCodes(order);
            applyAutoTransition(order, OrderStatus.COMPLETED, changedBy, "auto-paid-all-items-done");
            if (order.getTableSession() != null) {
                tableSessionService.closeSession(
                        order.getTableSession().getId(), TableSessionStatus.CLOSED, "Order settled");
            } else {
                orderTableSyncService.recalcTableStatus(order);
            }
            orderCacheInvalidationService.evictAfterOrderMutation(order, tableCodes);
            eventPublisher.publishEvent(new OrderSettledEvent(
                    order.getId(), order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today()));
        } else if (allDone && order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            applyAutoTransition(order, OrderStatus.AWAITING_PAYMENT, changedBy, "auto-all-items-done");
        } else if (!allDone && order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            applyAutoTransition(order, OrderStatus.SERVING, changedBy, "auto-items-reopened");
        } else if (anyCookingOrFinished && order.getStatus() == OrderStatus.PENDING) {
            applyAutoTransition(order, OrderStatus.SERVING, changedBy, "auto-kitchen-started");
        }
    }

    private void ensureNoPendingPayment(Order order, String message) {
        if (orderPaymentPort != null
                && order.getId() != null
                && orderPaymentPort.hasPendingPaymentForOrder(order.getId())) {
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_IN_PROGRESS, message);
        }
    }

    private void applyAutoTransition(Order order, OrderStatus targetStatus, User changedBy, String reason) {
        OrderStatus fromStatus = order.getStatus();

        orderStateFactory.validateTransition(fromStatus, targetStatus);

        OrderState state = orderStateFactory.getState(targetStatus);
        state.handleRequest(order);

        orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), changedBy, reason);

        orderRepository.save(order);

        log.info("Order #{} auto-transitioned from {} to {} ({})", order.getId(), fromStatus, targetStatus, reason);
    }

    private void restoreReservableItems(Order order) {
        order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.FINISHED)
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .forEach(inventoryReservationService::restoreOrderItem);
    }
}
