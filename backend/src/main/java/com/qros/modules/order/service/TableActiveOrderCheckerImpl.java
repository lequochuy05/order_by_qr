package com.qros.modules.order.service;

import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.repository.projection.ActiveOrderLockProjection;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.service.TableActiveOrderChecker;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableActiveOrderCheckerImpl implements TableActiveOrderChecker {

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT);
    private static final List<String> ACTIVE_ORDER_STATUS_NAMES =
            ACTIVE_ORDER_STATUSES.stream().map(Enum::name).toList();

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public boolean hasActiveOrders(Long tableId) {
        return orderRepository.existsByTableIdAndStatusIn(tableId, ACTIVE_ORDER_STATUSES);
    }

    @Override
    public boolean hasActiveOrdersForSession(Long sessionId) {
        return orderRepository.existsByTableSessionIdAndStatusIn(sessionId, ACTIVE_ORDER_STATUSES);
    }

    @Override
    public boolean hasPaymentInProgressForSession(Long sessionId) {
        return paymentTransactionRepository.existsByOrderTableSessionIdAndStatusIn(
                sessionId, List.of(PaymentTransactionStatus.CREATING, PaymentTransactionStatus.PENDING));
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
}
