package com.qros.modules.order.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.service.TableActiveOrderChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableActiveOrderCheckerImpl implements TableActiveOrderChecker {

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT);

    private final OrderRepository orderRepository;

    @Override
    public boolean hasActiveOrders(Long tableId) {
        return orderRepository.existsByTableIdAndStatusIn(tableId, ACTIVE_ORDER_STATUSES);
    }

    @Override
    public void attachActiveOrderToSession(Long tableId, TableSession session) {
        if (tableId == null || session == null) {
            return;
        }

        List<Order> activeOrders = orderRepository.findActiveByTableIdForUpdate(tableId, ACTIVE_ORDER_STATUSES);

        if (activeOrders == null || activeOrders.isEmpty()) {
            return;
        }

        activeOrders.stream()
                .filter(order -> order.getTableSession() == null)
                .findFirst()
                .ifPresent(order -> {
                    order.setTableSession(session);
                    orderRepository.save(order);
                    log.info("Attached staff-created order {} to table session {}", order.getId(), session.getId());
                });
    }
}
