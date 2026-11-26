package com.qros.modules.order.service;

import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.listener.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.service.TableSessionService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderItemWorkflowService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserLookupService userRepository;
    private final OrderInventoryReservationPort inventoryReservationPort;
    private final OrderPricingService orderPricingService;
    private final OrderAuditService orderAuditService;
    private final OrderStatusService orderStatusService;
    private final OrderTableSyncService orderTableSyncService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderMapper orderMapper;
    private final OrderPaymentPort orderPaymentPort;
    private final TableSessionService tableSessionService;

    public OrderItemWorkflowService(
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository,
            UserLookupService userRepository,
            OrderInventoryReservationPort inventoryReservationPort,
            OrderPricingService orderPricingService,
            OrderAuditService orderAuditService,
            OrderStatusService orderStatusService,
            OrderTableSyncService orderTableSyncService,
            OrderCacheInvalidationService orderCacheInvalidationService,
            ApplicationEventPublisher eventPublisher,
            OrderMapper orderMapper) {
        this(
                orderItemRepository,
                orderRepository,
                userRepository,
                inventoryReservationPort,
                orderPricingService,
                orderAuditService,
                orderStatusService,
                orderTableSyncService,
                orderCacheInvalidationService,
                eventPublisher,
                orderMapper,
                null,
                null);
    }

    @Autowired
    public OrderItemWorkflowService(
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository,
            UserLookupService userRepository,
            OrderInventoryReservationPort inventoryReservationPort,
            OrderPricingService orderPricingService,
            OrderAuditService orderAuditService,
            OrderStatusService orderStatusService,
            OrderTableSyncService orderTableSyncService,
            OrderCacheInvalidationService orderCacheInvalidationService,
            ApplicationEventPublisher eventPublisher,
            OrderMapper orderMapper,
            OrderPaymentPort orderPaymentPort,
            TableSessionService tableSessionService) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.inventoryReservationPort = inventoryReservationPort;
        this.orderPricingService = orderPricingService;
        this.orderAuditService = orderAuditService;
        this.orderStatusService = orderStatusService;
        this.orderTableSyncService = orderTableSyncService;
        this.orderCacheInvalidationService = orderCacheInvalidationService;
        this.eventPublisher = eventPublisher;
        this.orderMapper = orderMapper;
        this.orderPaymentPort = orderPaymentPort;
        this.tableSessionService = tableSessionService;
    }

    @Transactional
    public void cancelOrderItem(@NonNull Long itemId) {
        cancelOrderItem(itemId, null, null);
    }

    @Transactional
    public void cancelOrderItem(@NonNull Long itemId, String actorEmail, String reason) {
        OrderItem item = orderItemRepository
                .findDetailByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        Order order = item.getOrder();

        validateOrderMutableForEditing(order);

        if (!item.canBeCancelled()) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE, "Cannot cancel this item in current status: " + item.getStatus());
        }

        OrderItemStatus fromStatus = item.getStatus();

        inventoryReservationPort.restoreOrderItem(item);

        item.markStatus(OrderItemStatus.CANCELLED);
        orderItemRepository.saveAndFlush(item);

        User changedBy = resolveUserByEmail(actorEmail);
        String auditReason = org.springframework.util.StringUtils.hasText(reason) ? reason.trim() : "item-cancelled";
        orderAuditService.recordItemStatus(item, fromStatus, OrderItemStatus.CANCELLED, changedBy, auditReason);

        ReconciledOrder reconciled = reconcileOrderAfterItemMutation(order, null, true);

        orderCacheInvalidationService.evictAfterOrderMutation(reconciled.order(), reconciled.tableCodes());
        eventPublisher.publishEvent(new OrderChangeEvent());
    }

    @Transactional
    public void updateItemStatus(@NonNull Long itemId, @NonNull OrderItemStatus newStatus, Long userId) {
        OrderItem item = orderItemRepository
                .findDetailByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        Order order = item.getOrder();

        validateOrderMutable(order);

        OrderItemStatus fromStatus = item.getStatus();

        if (fromStatus == newStatus) {
            return;
        }

        if (newStatus == OrderItemStatus.CANCELLED || fromStatus == OrderItemStatus.CANCELLED) {
            ensureOrderNotPaidOrPaying(order);
        }

        User changedBy = resolveUser(userId);

        validateItemStatusTransition(fromStatus, newStatus);

        if (newStatus == OrderItemStatus.CANCELLED && fromStatus != OrderItemStatus.CANCELLED) {
            if (!item.canBeCancelled()) {
                throw new BusinessException(
                        ErrorCode.ORDER_INVALID_STATE, "Cannot cancel this item in current status: " + fromStatus);
            }

            inventoryReservationPort.restoreOrderItem(item);
        } else if (fromStatus == OrderItemStatus.CANCELLED && newStatus != OrderItemStatus.CANCELLED) {
            inventoryReservationPort.reserveForOrderItemQuantity(item, item.getQuantity());
        } else if (newStatus == OrderItemStatus.FINISHED && fromStatus != OrderItemStatus.FINISHED) {
            inventoryReservationPort.consumeForOrderItem(item);
        }

        item.markStatus(newStatus);
        orderItemRepository.saveAndFlush(item);

        orderAuditService.recordItemStatus(item, fromStatus, newStatus, changedBy, "kitchen-status-update");

        boolean totalsDirty = newStatus == OrderItemStatus.CANCELLED || fromStatus == OrderItemStatus.CANCELLED;
        ReconciledOrder reconciled = reconcileOrderAfterItemMutation(order, changedBy, totalsDirty);

        orderCacheInvalidationService.evictAfterOrderMutation(reconciled.order(), reconciled.tableCodes());
        eventPublisher.publishEvent(new OrderChangeEvent());
    }

    @Transactional
    public void updateBatchItemStatus(@NonNull List<Long> itemIds, @NonNull OrderItemStatus newStatus, Long userId) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }

        // Canonical ascending item-ID locking prevents PostgreSQL AB-BA deadlocks across concurrent chefs.
        List<Long> sortedItemIds = itemIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (sortedItemIds.isEmpty()) {
            return;
        }

        User changedBy = resolveUser(userId);
        java.util.Map<Long, Boolean> dirtyTotalsByOrderId = new java.util.TreeMap<>();

        for (Long itemId : sortedItemIds) {
            OrderItem item = orderItemRepository
                    .findDetailByIdForUpdate(itemId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

            Order order = item.getOrder();
            validateOrderMutable(order);

            OrderItemStatus fromStatus = item.getStatus();
            if (fromStatus == newStatus) {
                continue;
            }

            if (newStatus == OrderItemStatus.CANCELLED || fromStatus == OrderItemStatus.CANCELLED) {
                ensureOrderNotPaidOrPaying(order);
            }

            validateItemStatusTransition(fromStatus, newStatus);

            if (newStatus == OrderItemStatus.CANCELLED && fromStatus != OrderItemStatus.CANCELLED) {
                if (!item.canBeCancelled()) {
                    throw new BusinessException(
                            ErrorCode.ORDER_INVALID_STATE, "Cannot cancel this item in current status: " + fromStatus);
                }
                inventoryReservationPort.restoreOrderItem(item);
            } else if (fromStatus == OrderItemStatus.CANCELLED && newStatus != OrderItemStatus.CANCELLED) {
                inventoryReservationPort.reserveForOrderItemQuantity(item, item.getQuantity());
            } else if (newStatus == OrderItemStatus.FINISHED && fromStatus != OrderItemStatus.FINISHED) {
                inventoryReservationPort.consumeForOrderItem(item);
            }

            item.markStatus(newStatus);
            orderItemRepository.saveAndFlush(item);
            orderAuditService.recordItemStatus(item, fromStatus, newStatus, changedBy, "kitchen-batch-status-update");

            boolean totalsDirty = newStatus == OrderItemStatus.CANCELLED || fromStatus == OrderItemStatus.CANCELLED;
            dirtyTotalsByOrderId.merge(order.getId(), totalsDirty, (a, b) -> a || b);
        }

        if (dirtyTotalsByOrderId.isEmpty()) {
            return;
        }

        // Reconcile each affected parent order once in ascending order-ID sequence and emit a single event.
        for (java.util.Map.Entry<Long, Boolean> entry : dirtyTotalsByOrderId.entrySet()) {
            Order stub = new Order();
            stub.setId(entry.getKey());
            ReconciledOrder reconciled = reconcileOrderAfterItemMutation(stub, changedBy, entry.getValue());
            orderCacheInvalidationService.evictAfterOrderMutation(reconciled.order(), reconciled.tableCodes());
        }
        eventPublisher.publishEvent(new OrderChangeEvent());
    }

    @Transactional
    public void markItemPrepared(@NonNull Long itemId, Long userId) {
        updateItemStatus(itemId, OrderItemStatus.FINISHED, userId);
    }

    @Transactional
    public OrderResponse updateOrderItem(@NonNull Long itemId, Integer quantity, String notes) {
        OrderItem item = orderItemRepository
                .findDetailByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        Order order = item.getOrder();

        validateOrderMutableForEditing(order);

        if (!item.canBeUpdated()) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE, "Cannot update item in current status: " + item.getStatus());
        }

        if (quantity != null) {
            int oldQuantity = item.getQuantity();
            inventoryReservationPort.adjustReservationForQuantity(item, oldQuantity, quantity);

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

    private ReconciledOrder reconcileOrderAfterItemMutation(Order order, User changedBy, boolean totalsDirty) {
        Order lockedOrder = orderRepository
                .findWorkflowByIdForUpdate(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        List<String> tableCodes = orderCacheInvalidationService.resolveTableCodes(lockedOrder);

        if (totalsDirty) {
            orderPricingService.recalculateOrderTotals(lockedOrder);
        }

        boolean hasBillableItems = lockedOrder.getOrderItems().stream().anyMatch(OrderItem::isBillable);

        if (!hasBillableItems) {
            OrderStatus fromStatus = lockedOrder.getStatus();

            lockedOrder.setStatus(OrderStatus.CANCELLED);
            lockedOrder.setPaymentStatus(PaymentStatus.CANCELLED);

            orderAuditService.recordOrderStatus(
                    lockedOrder, fromStatus, lockedOrder.getStatus(), changedBy, "all-items-cancelled");

            Order saved = orderRepository.save(lockedOrder);
            if (tableSessionService != null && saved.getTableSession() != null) {
                tableSessionService.closeSession(
                        saved.getTableSession().getId(), TableSessionStatus.CANCELLED, "all-items-cancelled");
            } else {
                orderTableSyncService.recalcTableStatus(saved);
            }
            return new ReconciledOrder(saved, tableCodes);
        }

        orderStatusService.tryAutoPromoteOrder(lockedOrder, changedBy);

        Order saved = orderRepository.save(lockedOrder);
        if (saved.getStatus() != OrderStatus.COMPLETED) {
            orderTableSyncService.recalcTableStatus(saved);
        }
        return new ReconciledOrder(saved, tableCodes);
    }

    private void validateOrderMutable(Order order) {
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE, "Cannot update items of a completed or cancelled order");
        }
    }

    private void validateOrderMutableForEditing(Order order) {
        validateOrderMutable(order);
        ensureOrderNotPaidOrPaying(order);
    }

    private void ensureOrderNotPaidOrPaying(Order order) {
        if (order.isPaid()) {
            throw new BusinessException(
                    ErrorCode.ORDER_ALREADY_PAID, "Cannot modify or cancel items of an already paid order");
        }
        if (orderPaymentPort != null
                && order.getId() != null
                && orderPaymentPort.hasPendingPaymentForOrder(order.getId())) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_IN_PROGRESS,
                    "Cannot modify or cancel items while an online payment is in progress");
        }
    }

    private record ReconciledOrder(Order order, List<String> tableCodes) {}

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }

        return userRepository.findById(userId).orElse(null);
    }

    private User resolveUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
    }

    private void validateItemStatusTransition(OrderItemStatus from, OrderItemStatus to) {
        if (from == to) {
            return;
        }

        boolean valid = switch (from) {
            case PENDING ->
                to == OrderItemStatus.COOKING || to == OrderItemStatus.FINISHED || to == OrderItemStatus.CANCELLED;
            case COOKING -> to == OrderItemStatus.FINISHED || to == OrderItemStatus.CANCELLED;
            case FINISHED -> false;
            case CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_ITEM_STATUS, "Cannot transition item from " + from + " to " + to);
        }
    }
}
