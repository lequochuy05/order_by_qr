package com.qros.modules.kitchen.dto.response;

import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record KitchenOrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal finalAmount,
        TableSummary table,
        List<KitchenOrderItemResponse> orderItems,
        LocalDateTime createdAt) {

    public record TableSummary(Long id, String tableNumber) {}

    public record KitchenOrderItemResponse(
            Long id,
            MenuItemSummary menuItem,
            ComboSummary combo,
            BigDecimal unitPrice,
            int quantity,
            String notes,
            boolean prepared,
            OrderItemStatus status,
            List<KitchenOrderItemOptionResponse> options,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    public record MenuItemSummary(Long id, String name, CategorySummary category) {}

    public record CategorySummary(String name) {}

    public record ComboSummary(Long id, String name, BigDecimal price) {}

    public record KitchenOrderItemOptionResponse(
            Long valueId, String optionName, String optionValueName, BigDecimal extraPrice) {}
}
