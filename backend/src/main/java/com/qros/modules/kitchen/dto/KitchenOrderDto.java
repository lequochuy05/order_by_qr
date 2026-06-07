package com.qros.modules.kitchen.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record KitchenOrderDto(
        Long id,
        String status,
        BigDecimal finalAmount,
        TableSummary table,
        List<KitchenOrderItemDto> orderItems,
        LocalDateTime createdAt) {

    public record TableSummary(Long id, String tableNumber) {
    }

    public record KitchenOrderItemDto(
            Long id,
            MenuItemSummary menuItem,
            ComboSummary combo,
            BigDecimal unitPrice,
            int quantity,
            String notes,
            boolean prepared,
            String status,
            List<KitchenOrderItemOptionDto> options,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record MenuItemSummary(Long id, String name, CategorySummary category) {
    }

    public record CategorySummary(String name) {
    }

    public record ComboSummary(Long id, String name, BigDecimal price) {
    }

    public record KitchenOrderItemOptionDto(
            Long valueId,
            String optionName,
            String optionValueName,
            BigDecimal extraPrice) {
    }
}
