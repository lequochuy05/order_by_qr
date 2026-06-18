package com.qros.modules.kitchen.service;

import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.kitchen.mapper.KitchenMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderService;
import com.qros.shared.time.AppTime;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KitchenService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final KitchenMapper kitchenMapper;

    @Transactional(readOnly = true)
    public List<KitchenOrderResponse> getKitchenOrders() {
        LocalDateTime recentlyFinishedCutoff = AppTime.now().minusMinutes(15);

        return orderRepository
                .findKitchenOrders(
                        List.of(OrderStatus.PENDING, OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT),
                        List.of(OrderItemStatus.PENDING, OrderItemStatus.COOKING),
                        OrderItemStatus.FINISHED,
                        recentlyFinishedCutoff)
                .stream()
                .map(order -> toVisibleKitchenOrder(order, recentlyFinishedCutoff))
                .filter(response -> !response.orderItems().isEmpty())
                .sorted(Comparator.comparing(KitchenOrderResponse::createdAt))
                .toList();
    }

    @Transactional
    public void updateItemStatus(@NonNull Long itemId, @NonNull OrderItemStatus status, Long userId) {
        orderService.updateItemStatus(itemId, status, userId);
    }

    @Transactional
    public void markItemPrepared(@NonNull Long itemId, Long userId) {
        orderService.markItemPrepared(itemId, userId);
    }

    private KitchenOrderResponse toVisibleKitchenOrder(Order order, LocalDateTime cutoff) {
        List<OrderItem> visibleItems = order.getOrderItems().stream()
                .filter(item -> isVisibleOnKitchen(item, cutoff))
                .toList();

        return kitchenMapper.toResponse(order, visibleItems);
    }

    private boolean isVisibleOnKitchen(OrderItem item, LocalDateTime cutoff) {
        return item.getStatus() == OrderItemStatus.PENDING
                || item.getStatus() == OrderItemStatus.COOKING
                || isRecentlyFinished(item, cutoff);
    }

    private boolean isRecentlyFinished(OrderItem item, LocalDateTime cutoff) {
        return item.getStatus() == OrderItemStatus.FINISHED
                && item.getUpdatedAt() != null
                && !item.getUpdatedAt().isBefore(cutoff);
    }
}
