package com.qros.modules.order.service;

import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.table.service.TableActiveOrderChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TableActiveOrderCheckerImpl implements TableActiveOrderChecker {

    private final OrderRepository orderRepository;

    @Override
    public boolean hasActiveOrders(Long tableId) {
        return orderRepository.existsByTableIdAndStatusIn(
                tableId,
                List.of(OrderStatus.PENDING, OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT)
        );
    }
}
