package com.qros.modules.order.mapper;

import com.qros.modules.order.dto.response.OrderCategorySummaryResponse;
import com.qros.modules.order.dto.response.OrderComboSummaryResponse;
import com.qros.modules.order.dto.response.OrderItemOptionResponse;
import com.qros.modules.order.dto.response.OrderItemResponse;
import com.qros.modules.order.dto.response.OrderMenuItemSummaryResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.OrderTableSummaryResponse;
import com.qros.modules.order.dto.response.PublicOrderResponse;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.OrderItemOption;
import com.qros.modules.order.model.enums.OrderItemType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getVoucherCode(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getPaidAmount(),
                order.getBusinessDate(),
                order.getOrderType(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getPaidBy() != null ? order.getPaidBy().getFullName() : null,
                order.getPaymentTime(),
                toTableSummary(order),
                toItemResponses(order),
                order.getCreatedAt());
    }

    public PublicOrderResponse toPublicResponse(Order order) {
        if (order == null) {
            return null;
        }

        return new PublicOrderResponse(
                order.getId(),
                order.getStatus(),
                order.getFinalAmount(),
                toPublicTable(order),
                order.getOrderItems() == null
                        ? List.of()
                        : order.getOrderItems().stream()
                                .map(this::toPublicOrderItem)
                                .toList(),
                order.getCreatedAt());
    }

    private OrderTableSummaryResponse toTableSummary(Order order) {
        if (order.getTable() == null) {
            return null;
        }

        return new OrderTableSummaryResponse(
                order.getTable().getId(), order.getTable().getTableNumber());
    }

    private List<OrderItemResponse> toItemResponses(Order order) {
        if (order.getOrderItems() == null) {
            return List.of();
        }

        return order.getOrderItems().stream().map(this::toItemResponse).toList();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getBatch() != null ? item.getBatch().getId() : null,
                toMenuItemSummary(item),
                toComboSummary(item),
                item.getItemNameSnapshot(),
                item.getItemType(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal(),
                item.getNotes(),
                item.isPrepared(),
                item.getStatus(),
                toOptionResponses(item),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    private OrderMenuItemSummaryResponse toMenuItemSummary(OrderItem item) {
        if (item.getMenuItem() != null) {
            return new OrderMenuItemSummaryResponse(
                    item.getMenuItem().getId(),
                    item.getMenuItem().getName(),
                    item.getMenuItem().getCategory() != null
                            ? new OrderCategorySummaryResponse(
                                    item.getMenuItem().getCategory().getName())
                            : null);
        }

        if (item.getItemType() == OrderItemType.MENU_ITEM) {
            return new OrderMenuItemSummaryResponse(null, item.getItemNameSnapshot(), null);
        }

        return null;
    }

    private OrderComboSummaryResponse toComboSummary(OrderItem item) {
        if (item.getCombo() != null) {
            return new OrderComboSummaryResponse(
                    item.getCombo().getId(),
                    item.getCombo().getName(),
                    item.getCombo().getPrice());
        }

        if (item.getItemType() == OrderItemType.COMBO) {
            return new OrderComboSummaryResponse(null, item.getItemNameSnapshot(), item.getUnitPrice());
        }

        return null;
    }

    private List<OrderItemOptionResponse> toOptionResponses(OrderItem item) {
        if (item.getOrderItemOptions() == null) {
            return List.of();
        }

        return item.getOrderItemOptions().stream().map(this::toOptionResponse).toList();
    }

    private OrderItemOptionResponse toOptionResponse(OrderItemOption option) {
        return new OrderItemOptionResponse(
                option.getItemOptionValue() != null
                        ? option.getItemOptionValue().getId()
                        : null,
                option.getOptionName(),
                option.getOptionValueName(),
                option.getExtraPrice());
    }

    private PublicOrderResponse.Table toPublicTable(Order order) {
        if (order.getTable() == null) {
            return null;
        }

        return new PublicOrderResponse.Table(
                order.getTable().getId(), order.getTable().getTableNumber());
    }

    private PublicOrderResponse.OrderItem toPublicOrderItem(OrderItem item) {
        return new PublicOrderResponse.OrderItem(
                item.getId(),
                toPublicMenuItemSummary(item),
                toPublicComboSummary(item),
                item.getItemType(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal(),
                item.getNotes(),
                item.getStatus(),
                toPublicOptionResponses(item));
    }

    private PublicOrderResponse.MenuItemSummary toPublicMenuItemSummary(OrderItem item) {
        if (item.getMenuItem() != null) {
            return new PublicOrderResponse.MenuItemSummary(
                    item.getMenuItem().getId(),
                    item.getMenuItem().getName(),
                    item.getMenuItem().getCategory() != null
                            ? new PublicOrderResponse.CategoryName(
                                    item.getMenuItem().getCategory().getName())
                            : null);
        }

        if (item.getItemType() == OrderItemType.MENU_ITEM) {
            return new PublicOrderResponse.MenuItemSummary(null, item.getItemNameSnapshot(), null);
        }

        return null;
    }

    private PublicOrderResponse.ComboSummary toPublicComboSummary(OrderItem item) {
        if (item.getCombo() != null) {
            return new PublicOrderResponse.ComboSummary(
                    item.getCombo().getId(),
                    item.getCombo().getName(),
                    item.getCombo().getPrice());
        }

        if (item.getItemType() == OrderItemType.COMBO) {
            return new PublicOrderResponse.ComboSummary(null, item.getItemNameSnapshot(), item.getUnitPrice());
        }

        return null;
    }

    private List<PublicOrderResponse.OrderItemOption> toPublicOptionResponses(OrderItem item) {
        if (item.getOrderItemOptions() == null) {
            return List.of();
        }

        return item.getOrderItemOptions().stream()
                .map(option -> new PublicOrderResponse.OrderItemOption(
                        option.getOptionName(), option.getOptionValueName(), option.getExtraPrice()))
                .toList();
    }
}
