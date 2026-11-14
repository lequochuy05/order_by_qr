package com.qros.modules.kitchen.mapper;

import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderBatch;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemType;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class KitchenMapper {

    public KitchenOrderResponse toResponse(Order order, List<OrderItem> visibleItems) {
        Map<Long, Integer> batchNumbers = computeBatchNumbers(order);
        return new KitchenOrderResponse(
                order.getId(),
                order.getStatus(),
                order.getFinalAmount(),
                order.getTable() != null
                        ? new KitchenOrderResponse.TableSummary(
                                order.getTable().getId(), order.getTable().getTableNumber())
                        : null,
                order.getOrderType(),
                visibleItems.stream()
                        .sorted(Comparator.comparing(
                                OrderItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(item -> toItemResponse(item, batchNumbers))
                        .toList(),
                order.getCreatedAt());
    }

    private KitchenOrderResponse.KitchenOrderItemResponse toItemResponse(
            OrderItem item, Map<Long, Integer> batchNumbers) {
        Long batchId = item.getBatch() != null ? item.getBatch().getId() : null;
        Integer batchNumber = batchId != null ? batchNumbers.getOrDefault(batchId, 1) : 1;
        return new KitchenOrderResponse.KitchenOrderItemResponse(
                item.getId(),
                batchId,
                batchNumber,
                toMenuItemSummary(item),
                toComboSummary(item),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getNotes(),
                item.isPrepared(),
                item.getStatus(),
                item.getOrderItemOptions().stream()
                        .map(option -> new KitchenOrderResponse.KitchenOrderItemOptionResponse(
                                option.getItemOptionValue() != null
                                        ? option.getItemOptionValue().getId()
                                        : null,
                                option.getOptionName(),
                                option.getOptionValueName(),
                                option.getExtraPrice()))
                        .toList(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    private Map<Long, Integer> computeBatchNumbers(Order order) {
        Set<OrderBatch> batches = new LinkedHashSet<>();
        if (order.getOrderBatches() != null) {
            batches.addAll(order.getOrderBatches());
        }
        if (order.getOrderItems() != null) {
            for (OrderItem oi : order.getOrderItems()) {
                if (oi.getBatch() != null) {
                    batches.add(oi.getBatch());
                }
            }
        }
        if (batches.isEmpty()) {
            return Map.of();
        }
        List<OrderBatch> sorted = batches.stream()
                .filter(b -> b.getId() != null)
                .sorted(Comparator.comparing(
                                OrderBatch::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OrderBatch::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<Long, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            result.put(sorted.get(i).getId(), i + 1);
        }
        return result;
    }

    private KitchenOrderResponse.MenuItemSummary toMenuItemSummary(OrderItem item) {
        if (item.getMenuItem() != null) {
            return new KitchenOrderResponse.MenuItemSummary(
                    item.getMenuItem().getId(),
                    item.getMenuItem().getName(),
                    item.getMenuItem().getCategory() != null
                            ? new KitchenOrderResponse.CategorySummary(
                                    item.getMenuItem().getCategory().getName())
                            : null);
        }

        if (item.getItemType() == OrderItemType.MENU_ITEM) {
            return new KitchenOrderResponse.MenuItemSummary(null, item.getItemNameSnapshot(), null);
        }

        return null;
    }

    private KitchenOrderResponse.ComboSummary toComboSummary(OrderItem item) {
        if (item.getCombo() != null) {
            return new KitchenOrderResponse.ComboSummary(
                    item.getCombo().getId(),
                    item.getCombo().getName(),
                    item.getCombo().getPrice());
        }

        if (item.getItemType() == OrderItemType.COMBO) {
            return new KitchenOrderResponse.ComboSummary(null, item.getItemNameSnapshot(), item.getUnitPrice());
        }

        return null;
    }
}
