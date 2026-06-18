package com.qros.modules.kitchen.mapper;

import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemType;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KitchenMapper {

    public KitchenOrderResponse toResponse(Order order, List<OrderItem> visibleItems) {
        return new KitchenOrderResponse(
                order.getId(),
                order.getStatus(),
                order.getFinalAmount(),
                order.getTable() != null
                        ? new KitchenOrderResponse.TableSummary(
                                order.getTable().getId(), order.getTable().getTableNumber())
                        : null,
                visibleItems.stream()
                        .sorted(Comparator.comparing(OrderItem::getCreatedAt))
                        .map(this::toItemResponse)
                        .toList(),
                order.getCreatedAt());
    }

    private KitchenOrderResponse.KitchenOrderItemResponse toItemResponse(OrderItem item) {
        return new KitchenOrderResponse.KitchenOrderItemResponse(
                item.getId(),
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
