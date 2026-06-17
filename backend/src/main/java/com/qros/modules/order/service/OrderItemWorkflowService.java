package com.qros.modules.order.service;

import com.qros.modules.inventory.service.InventoryReservationService;
import org.springframework.context.ApplicationEventPublisher;
import com.qros.shared.event.DomainEvents.*;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderItemWorkflowService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryReservationService inventoryReservationService;
    private final OrderPricingService orderPricingService;
    private final OrderAuditService orderAuditService;
    private final OrderStatusService orderStatusService;
    private final OrderTableSyncService orderTableSyncService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderMapper orderMapper;

    @Transactional
    public void cancelOrderItem(@NonNull Long itemId) {
        OrderItem item = orderItemRepository.findDetailByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        Order order = item.getOrder();

        validateOrderMutable(order);

        if (!item.canBeCancelled()) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE,
                    "Cannot cancel this item in current status: " + item.getStatus());
        }

        OrderItemStatus fromStatus = item.getStatus();

        inventoryReservationService.restoreOrderItem(item);

        item.markStatus(OrderItemStatus.CANCELLED);
        orderItemRepository.saveAndFlush(item);

        orderAuditService.recordItemStatus(
                item,
                fromStatus,
                OrderItemStatus.CANCELLED,
                null,
                "item-cancelled");

        Order saved = reconcileOrderAfterItemMutation(order, null);

        orderCacheInvalidationService.evictAfterOrderMutation(saved);
        eventPublisher.publishEvent(new OrderChangeEvent());
    }

    @Transactional
    public void updateItemStatus(@NonNull Long itemId, @NonNull OrderItemStatus newStatus) {
        updateItemStatus(itemId, newStatus, null);
    }

    @Transactional
    public void updateItemStatus(@NonNull Long itemId, @NonNull OrderItemStatus newStatus, Long userId) {
        OrderItem item = orderItemRepository.findDetailByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        Order order = item.getOrder();

        validateOrderMutable(order);

        OrderItemStatus fromStatus = item.getStatus();

        if (fromStatus == newStatus) {
            return;
        }

        User changedBy = resolveUser(userId);

        validateItemStatusTransition(fromStatus, newStatus);

        if (newStatus == OrderItemStatus.CANCELLED && fromStatus != OrderItemStatus.CANCELLED) {
            if (!item.canBeCancelled()) {
                throw new BusinessException(
                        ErrorCode.ORDER_INVALID_STATE,
                        "Cannot cancel this item in current status: " + fromStatus);
            }

            inventoryReservationService.restoreOrderItem(item);
        } else if (fromStatus == OrderItemStatus.CANCELLED && newStatus != OrderItemStatus.CANCELLED) {
            inventoryReservationService.reserveForOrderItem(item, item.getQuantity());
        } else if (newStatus == OrderItemStatus.FINISHED && fromStatus != OrderItemStatus.FINISHED) {
            inventoryReservationService.consumeForOrderItem(item);
        }

        item.markStatus(newStatus);
        orderItemRepository.saveAndFlush(item);

        orderAuditService.recordItemStatus(
                item,
                fromStatus,
                newStatus,
                changedBy,
                "kitchen-status-update");

        Order saved = reconcileOrderAfterItemMutation(order, changedBy);

        orderCacheInvalidationService.evictAfterOrderMutation(saved);
        eventPublisher.publishEvent(new OrderChangeEvent());
    }

    @Transactional
    public void markItemPrepared(@NonNull Long itemId) {
        updateItemStatus(itemId, OrderItemStatus.FINISHED);
    }

    @Transactional
    public void markItemPrepared(@NonNull Long itemId, Long userId) {
        updateItemStatus(itemId, OrderItemStatus.FINISHED, userId);
    }

    @Transactional
    public OrderResponse updateOrderItem(@NonNull Long itemId, Integer quantity, String notes) {
        OrderItem item = orderItemRepository.findDetailByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        Order order = item.getOrder();

        validateOrderMutable(order);

        if (!item.canBeUpdated()) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE,
                    "Cannot update item in current status: " + item.getStatus());
        }

        if (quantity != null) {
            int oldQuantity = item.getQuantity();
            inventoryReservationService.adjustReservationForQuantity(item, oldQuantity, quantity);

            item.setQuantity(quantity);
            orderPricingService.recalculateLineTotal(item);
        }

        if (notes != null) {
            item.setNotes(notes.trim());
        }

        orderItemRepository.save(item);

        orderPricingService.recalculateOrderTotals(order);
        Order saved = orderRepository.save(order);

        orderTableSyncService.recalcTableStatus(saved);
        orderCacheInvalidationService.evictAfterOrderMutation(saved);
        eventPublisher.publishEvent(new OrderChangeEvent());

        return orderMapper.toResponse(saved);
    }

    private Order reconcileOrderAfterItemMutation(Order order, User changedBy) {
        Order lockedOrder = orderRepository.findByIdForUpdate(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        orderPricingService.recalculateOrderTotals(lockedOrder);

        boolean hasBillableItems = lockedOrder.getOrderItems().stream()
                .anyMatch(OrderItem::isBillable);

        if (!hasBillableItems) {
            OrderStatus fromStatus = lockedOrder.getStatus();

            lockedOrder.setStatus(OrderStatus.CANCELLED);
            lockedOrder.setPaymentStatus(PaymentStatus.CANCELLED);

            orderAuditService.recordOrderStatus(
                    lockedOrder,
                    fromStatus,
                    lockedOrder.getStatus(),
                    changedBy,
                    "all-items-cancelled");

            Order saved = orderRepository.save(lockedOrder);
            orderTableSyncService.recalcTableStatus(saved);
            return saved;
        }

        orderStatusService.tryAutoPromoteOrder(lockedOrder, changedBy);

        Order saved = orderRepository.save(lockedOrder);
        orderTableSyncService.recalcTableStatus(saved);
        return saved;
    }

    private void validateOrderMutable(Order order) {
        if (order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE,
                    "Cannot update items of a completed or cancelled order");
        }
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }

        return userRepository.findById(userId).orElse(null);
    }

    private void validateItemStatusTransition(OrderItemStatus from, OrderItemStatus to) {
        if (from == to) {
            return;
        }

        boolean valid = switch (from) {
            case PENDING -> to == OrderItemStatus.COOKING || to == OrderItemStatus.FINISHED || to == OrderItemStatus.CANCELLED;
            case COOKING -> to == OrderItemStatus.FINISHED;
            case FINISHED -> false;
            case CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_ITEM_STATUS,
                    "Cannot transition item from " + from + " to " + to);
        }
    }
}
