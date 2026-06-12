package com.qros.modules.order.service;

import com.qros.modules.inventory.service.InventoryReservationService;
import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.user.model.User;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderStateFactory orderStateFactory;
    private final OrderAuditService orderAuditService;
    private final OrderTableSyncService orderTableSyncService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final InventoryReservationService inventoryReservationService;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse updateStatus(@NonNull Long id, @NonNull OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.COMPLETED) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID,
                    "Use payment endpoint to complete an order");
        }

        if (targetStatus == OrderStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE,
                    "Use cancel endpoint to cancel an order");
        }

        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        try {
            orderStateFactory.validateTransition(order.getStatus(), targetStatus);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE, e.getMessage());
        }

        OrderStatus fromStatus = order.getStatus();

        OrderState state = orderStateFactory.getState(targetStatus);
        state.handleRequest(order);

        orderAuditService.recordOrderStatus(
                order,
                fromStatus,
                order.getStatus(),
                null,
                "manual-status-update");

        Order saved = orderRepository.save(order);

        orderTableSyncService.recalcTableStatus(saved);
        orderCacheInvalidationService.evictAfterOrderMutation(id);
        notificationService.notifyOrderChange();

        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(@NonNull Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.canBeCancelled()) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE,
                    "Cannot cancel this order in current state");
        }

        OrderStatus fromStatus = order.getStatus();

        order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.FINISHED)
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .forEach(item -> {
                    OrderItemStatus fromItemStatus = item.getStatus();

                    inventoryReservationService.restoreOrderItem(item);
                    item.markStatus(OrderItemStatus.CANCELLED);

                    orderAuditService.recordItemStatus(
                            item,
                            fromItemStatus,
                            OrderItemStatus.CANCELLED,
                            null,
                            "order-cancelled");
                });

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);

        orderAuditService.recordOrderStatus(
                order,
                fromStatus,
                order.getStatus(),
                null,
                "order-cancelled");

        Order saved = orderRepository.save(order);

        orderTableSyncService.recalcTableStatus(saved);
        orderCacheInvalidationService.evictAfterOrderMutation(id);
        notificationService.notifyOrderChange();

        return orderMapper.toResponse(saved);
    }

    @Transactional
    public void deleteOrder(@NonNull Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        restoreReservableItems(order);

        orderTableSyncService.releaseTable(order);

        orderRepository.delete(order);

        orderCacheInvalidationService.evictAfterOrderMutation(id);
        notificationService.notifyOrderChange();
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

        List<OrderItem> billableItems = order.getOrderItems().stream()
                .filter(OrderItem::isBillable)
                .toList();

        if (billableItems.isEmpty()) {
            return;
        }

        boolean allDone = billableItems.stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.FINISHED);

        boolean anyCookingOrFinished = billableItems.stream()
                .anyMatch(item -> item.getStatus() == OrderItemStatus.COOKING
                        || item.getStatus() == OrderItemStatus.FINISHED);

        if (allDone && order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            OrderStatus fromStatus = order.getStatus();

            order.setStatus(OrderStatus.AWAITING_PAYMENT);

            orderAuditService.recordOrderStatus(
                    order,
                    fromStatus,
                    order.getStatus(),
                    changedBy,
                    "auto-all-items-done");

            orderRepository.save(order);

            log.info("Order #{} auto-promoted to AWAITING_PAYMENT", order.getId());
        } else if (!allDone && order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            OrderStatus fromStatus = order.getStatus();

            order.setStatus(OrderStatus.SERVING);

            orderAuditService.recordOrderStatus(
                    order,
                    fromStatus,
                    order.getStatus(),
                    changedBy,
                    "auto-items-reopened");

            orderRepository.save(order);

            log.info("Order #{} auto-demoted to SERVING", order.getId());
        } else if (anyCookingOrFinished && order.getStatus() == OrderStatus.PENDING) {
            OrderStatus fromStatus = order.getStatus();

            order.setStatus(OrderStatus.SERVING);

            orderAuditService.recordOrderStatus(
                    order,
                    fromStatus,
                    order.getStatus(),
                    changedBy,
                    "auto-kitchen-started");

            orderRepository.save(order);

            log.info("Order #{} auto-promoted to SERVING", order.getId());
        }
    }

    private void restoreReservableItems(Order order) {
        order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.FINISHED)
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .forEach(inventoryReservationService::restoreOrderItem);
    }
}