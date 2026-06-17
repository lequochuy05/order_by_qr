package com.qros.modules.order.dto.response;

import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderItemType;
import com.qros.modules.order.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PublicOrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal finalAmount,
        Table table,
        List<OrderItem> items,
        LocalDateTime createdAt) {
    public record Table(Long id, String tableNumber) {}

    public record OrderItem(
            Long id,
            MenuItemSummary menuItem,
            ComboSummary combo,
            OrderItemType itemType,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal lineTotal,
            String notes,
            OrderItemStatus status,
            List<OrderItemOption> options) {}

    public record MenuItemSummary(Long id, String name, CategoryName category) {}

    public record CategoryName(String name) {}

    public record ComboSummary(Long id, String name, BigDecimal price) {}

    public record OrderItemOption(String optionName, String optionValueName, BigDecimal extraPrice) {}
}
