package com.qros.modules.order.mapper;

import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getVoucherCode(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getPaidAmount(),
                order.getBusinessDate(),
                order.getOrderType().name(),
                order.getPaymentStatus().name(),
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
                order.getPaidBy() != null ? order.getPaidBy().getFullName() : null,
                order.getPaymentTime(),
                order.getTable() != null
                        ? new OrderResponse.TableSummary(order.getTable().getId(), order.getTable().getTableNumber())
                        : null,
                order.getOrderItems().stream().map(item -> new OrderResponse.OrderItemResponse(
                        item.getId(),
                        item.getBatch() != null ? item.getBatch().getId() : null,
                        item.getMenuItem() != null ? new OrderResponse.MenuItemSummary(
                                item.getMenuItem().getId(),
                                item.getMenuItem().getName(),
                                item.getMenuItem().getCategory() != null
                                        ? new OrderResponse.CategorySummary(item.getMenuItem().getCategory().getName())
                                        : null)
                                : null,
                        item.getCombo() != null ? new OrderResponse.ComboSummary(
                                item.getCombo().getId(),
                                item.getCombo().getName(),
                                item.getCombo().getPrice()) : null,
                        item.getItemNameSnapshot(),
                        item.getItemType() != null ? item.getItemType().name() : null,
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal(),
                        item.getNotes(),
                        item.isPrepared(),
                        item.getStatus().name(),
                        item.getOrderItemOptions().stream().map(option -> new OrderResponse.OrderItemOptionResponse(
                                option.getItemOptionValue() != null ? option.getItemOptionValue().getId() : null,
                                option.getOptionName(),
                                option.getOptionValueName(),
                                option.getExtraPrice())).toList(),
                        item.getCreatedAt(),
                        item.getUpdatedAt()))
                        .toList(),
                order.getCreatedAt());
    }

    public PublicMenuResponse.Order toPublicResponse(Order order) {
        return new PublicMenuResponse.Order(
                order.getId(),
                order.getStatus().name(),
                order.getFinalAmount(),
                order.getTable() != null
                        ? new PublicMenuResponse.Table(order.getTable().getId(), order.getTable().getTableNumber())
                        : null,
                order.getOrderItems().stream().map(item -> new PublicMenuResponse.OrderItem(
                        item.getId(),
                        item.getMenuItem() != null ? new PublicMenuResponse.MenuItemSummary(
                                item.getMenuItem().getId(),
                                item.getMenuItem().getName(),
                                item.getMenuItem().getCategory() != null
                                        ? new PublicMenuResponse.CategoryName(item.getMenuItem().getCategory().getName())
                                        : null)
                                : item.getItemType() == OrderItem.OrderItemType.MENU_ITEM
                                        ? new PublicMenuResponse.MenuItemSummary(null, item.getItemNameSnapshot(), null)
                                : null,
                        item.getCombo() != null ? new PublicMenuResponse.ComboSummary(
                                item.getCombo().getId(),
                                item.getCombo().getName(),
                                item.getCombo().getPrice())
                                : item.getItemType() == OrderItem.OrderItemType.COMBO
                                        ? new PublicMenuResponse.ComboSummary(null, item.getItemNameSnapshot(),
                                                item.getUnitPrice())
                                        : null,
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getNotes(),
                        item.getStatus().name(),
                        item.getOrderItemOptions().stream().map(option -> new PublicMenuResponse.OrderItemOption(
                                option.getOptionName(),
                                option.getOptionValueName(),
                                option.getExtraPrice())).toList()))
                        .toList(),
                order.getCreatedAt());
    }
}
