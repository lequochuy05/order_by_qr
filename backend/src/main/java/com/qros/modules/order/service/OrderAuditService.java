package com.qros.modules.order.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.OrderItemStatusHistory;
import com.qros.modules.order.model.OrderStatusHistory;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderItemStatusHistoryRepository;
import com.qros.modules.order.repository.OrderStatusHistoryRepository;
import com.qros.modules.user.model.User;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderAuditService {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderItemStatusHistoryRepository orderItemStatusHistoryRepository;

    public void recordOrderStatus(Order order, OrderStatus from, OrderStatus to, User changedBy, String reason) {
        if (order == null || to == null || from == to) {
            return;
        }

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .changedAt(AppTime.now())
                .reason(reason)
                .build());
    }

    public void recordItemStatus(
            OrderItem item, OrderItemStatus from, OrderItemStatus to, User changedBy, String reason) {
        if (item == null || to == null || from == to) {
            return;
        }

        orderItemStatusHistoryRepository.save(OrderItemStatusHistory.builder()
                .orderItem(item)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .changedAt(AppTime.now())
                .reason(reason)
                .build());
    }
}
