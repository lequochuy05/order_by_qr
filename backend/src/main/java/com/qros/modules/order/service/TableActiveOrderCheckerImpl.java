package com.qros.modules.order.service;

import com.qros.modules.order.listener.OrderCacheInvalidationService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.repository.projection.ActiveOrderLockProjection;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.service.TableActiveOrderChecker;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableActiveOrderCheckerImpl implements TableActiveOrderChecker {

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = OrderStatus.ACTIVE_STATUSES;
    private static final List<String> ACTIVE_ORDER_STATUS_NAMES = OrderStatus.ACTIVE_STATUS_NAMES;

    private final OrderRepository orderRepository;
    private final OrderPaymentPort orderPaymentPort;
    private final OrderStateFactory orderStateFactory;
    private final OrderAuditService orderAuditService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;

    @Override
    public boolean hasActiveOrders(Long tableId) {
        return orderRepository.existsByTableIdAndStatusIn(tableId, ACTIVE_ORDER_STATUSES);
    }

    @Override
    public Set<Long> findTableIdsWithActiveOrders(Collection<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(orderRepository.findDistinctActiveTableIdsIn(tableIds, ACTIVE_ORDER_STATUSES));
    }

    @Override
    public boolean hasUnfinishedItems(Long tableId) {
        if (tableId == null) {
            return false;
        }
        return orderRepository.existsUnfinishedBillableItemByTableId(tableId, ACTIVE_ORDER_STATUSES);
    }

    @Override
    public boolean hasActiveOrdersForSession(Long sessionId) {
        return orderRepository.existsByTableSessionIdAndStatusIn(sessionId, ACTIVE_ORDER_STATUSES);
    }

    @Override
    public boolean hasPaymentInProgressForSession(Long sessionId) {
        return orderPaymentPort.hasPendingPaymentForTableSession(sessionId);
    }

    @Override
    public boolean hasBillableItemsForSession(Long sessionId) {
        return orderRepository.countBillableItemsByTableSessionId(sessionId, ACTIVE_ORDER_STATUSES) > 0;
    }

    @Override
    public int countActiveOrdersByTableIds(Collection<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return 0;
        }

        return (int) orderRepository.countActiveByTableIds(tableIds, ACTIVE_ORDER_STATUSES);
    }

    @Override
    public void attachActiveOrdersToSession(Collection<Long> tableIds, TableSession session) {
        if (tableIds == null || tableIds.isEmpty() || session == null) {
            return;
        }

        List<ActiveOrderLockProjection> activeOrders =
                orderRepository.findActiveOrderLocksByTableIdsForUpdate(tableIds, ACTIVE_ORDER_STATUS_NAMES);

        activeOrders.stream()
                .filter(order -> order.getTableSessionId() == null)
                .findFirst()
                .ifPresent(order -> {
                    orderRepository.attachTableSessionIfMissing(order.getId(), session);
                    log.info("Attached staff-created order {} to table session {}", order.getId(), session.getId());
                });
    }

    @Override
    @Transactional
    public void cancelEmptyActiveOrdersForSession(Long sessionId) {
        orderRepository.cancelEmptyActiveOrdersByTableSessionId(sessionId, ACTIVE_ORDER_STATUSES);
    }

    @Override
    @Transactional
    public void validateAndPrepareSessionForPaymentRequest(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "No active order found for this table");
        }

        Order order = orderRepository.findActiveByTableSessionIdForUpdate(sessionId, ACTIVE_ORDER_STATUSES).stream()
                .findFirst()
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "No active order found for this table"));

        boolean hasBillableItems = order.getOrderItems().stream().anyMatch(OrderItem::isBillable);
        if (!hasBillableItems) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "No billable items found for this order");
        }

        boolean hasUnfinishedItems = order.getOrderItems().stream()
                .filter(OrderItem::isBillable)
                .anyMatch(item -> item.getStatus() != OrderItemStatus.FINISHED);

        if (hasUnfinishedItems) {
            throw new BusinessException(
                    ErrorCode.ORDER_HAS_UNFINISHED_ITEMS,
                    "Cannot request payment: kitchen still has items being prepared");
        }

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            OrderStatus fromStatus = order.getStatus();
            orderStateFactory.validateTransition(fromStatus, OrderStatus.AWAITING_PAYMENT);
            orderStateFactory.getState(OrderStatus.AWAITING_PAYMENT).handleRequest(order);

            orderAuditService.recordOrderStatus(
                    order, fromStatus, order.getStatus(), null, "customer-requested-payment");
            Order saved = orderRepository.save(order);
            orderCacheInvalidationService.evictAfterOrderMutation(saved);
        }
    }
}
