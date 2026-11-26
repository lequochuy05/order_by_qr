package com.qros.modules.order.service;

import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Module-boundary lookup facade for orders and order items consumed by kitchen, recommendation, and AI modules.
 */
@Service
@RequiredArgsConstructor
public class OrderLookupService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public List<Order> findKitchenOrders(
            List<OrderStatus> statuses,
            List<OrderItemStatus> activeItemStatuses,
            OrderItemStatus finishedStatus,
            LocalDateTime recentlyFinishedCutoff) {
        return orderRepository.findKitchenOrders(statuses, activeItemStatuses, finishedStatus, recentlyFinishedCutoff);
    }

    public Page<Order> findAllOrders(@NonNull Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public List<Order> findOrdersByStatuses(@NonNull List<OrderStatus> statuses) {
        return orderRepository.findByStatusIn(statuses);
    }

    public List<Order> findDistinctOrdersByIds(Collection<Long> ids) {
        return orderRepository.findDistinctByIdIn(ids);
    }

    public List<MenuItem> findPopularAvailableMenuItems(Pageable pageable) {
        return orderItemRepository.findPopularAvailableMenuItems(pageable);
    }

    public List<MenuItem> findCrossSellAvailableMenuItems(Long itemId, Pageable pageable) {
        return orderItemRepository.findCrossSellAvailableMenuItems(itemId, pageable);
    }

    public List<Object[]> findFrequentlyPairedMenuItems(LocalDateTime from, Pageable pageable) {
        return orderItemRepository.findFrequentlyPairedMenuItems(from, pageable);
    }

    public boolean existsActiveOrderWithVoucherCode(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return false;
        }
        return orderRepository.existsByVoucherCodeIgnoreCaseAndStatusIn(
                voucherCode.trim(), OrderStatus.ACTIVE_STATUSES);
    }
}
