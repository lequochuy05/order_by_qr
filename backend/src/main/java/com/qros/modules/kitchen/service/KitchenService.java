package com.qros.modules.kitchen.service;

import com.qros.modules.kitchen.dto.KitchenOrderDto;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderStatusService;
import com.qros.shared.util.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitchenService {

    private final OrderRepository orderRepository;
    private final OrderStatusService orderStatusService;

    @Transactional(readOnly = true)
    public List<KitchenOrderDto> getKitchenOrders() {
        LocalDateTime recentlyFinishedCutoff = AppTime.now().minusMinutes(15);

        return orderRepository.findByStatusIn(List.of(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.SERVING,
                Order.OrderStatus.AWAITING_PAYMENT)).stream()
                .filter(order -> order.getOrderItems().stream()
                        .anyMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.PENDING
                                || item.getStatus() == OrderItem.OrderItemStatus.COOKING
                                || isRecentlyFinished(item, recentlyFinishedCutoff)))
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .map(this::toKitchenOrderDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void updateItemStatus(@NonNull Long itemId, @NonNull String status) {
        orderStatusService.updateItemStatus(itemId, status);
    }

    @Transactional
    public void markItemPrepared(@NonNull Long itemId) {
        orderStatusService.markItemPrepared(itemId);
    }

    private boolean isRecentlyFinished(OrderItem item, LocalDateTime cutoff) {
        return item.getStatus() == OrderItem.OrderItemStatus.FINISHED
                && item.getUpdatedAt() != null
                && !item.getUpdatedAt().isBefore(cutoff);
    }

    private KitchenOrderDto toKitchenOrderDto(Order order) {
        return new KitchenOrderDto(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getTable() != null
                        ? new KitchenOrderDto.TableSummary(order.getTable().getId(), order.getTable().getTableNumber())
                        : null,
                order.getOrderItems().stream().map(item -> new KitchenOrderDto.KitchenOrderItemDto(
                        item.getId(),
                        item.getMenuItem() != null ? new KitchenOrderDto.MenuItemSummary(
                                item.getMenuItem().getId(),
                                item.getMenuItem().getName(),
                                item.getMenuItem().getCategory() != null
                                        ? new KitchenOrderDto.CategorySummary(item.getMenuItem().getCategory().getName())
                                        : null)
                                : null,
                        item.getCombo() != null ? new KitchenOrderDto.ComboSummary(
                                item.getCombo().getId(),
                                item.getCombo().getName(),
                                item.getCombo().getPrice()) : null,
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getNotes(),
                        item.isPrepared(),
                        item.getStatus().name(),
                        item.getOrderItemOptions().stream()
                                .map(option -> new KitchenOrderDto.KitchenOrderItemOptionDto(
                                        option.getItemOptionValue() != null
                                                ? option.getItemOptionValue().getId()
                                                : null,
                                        option.getOptionName(),
                                        option.getOptionValueName(),
                                        option.getExtraPrice()))
                                .toList(),
                        item.getCreatedAt(),
                        item.getUpdatedAt()))
                        .toList(),
                order.getCreatedAt());
    }
}
