package com.qros.modules.inventory.dto.response;

public record InventorySummaryResponse(
        long totalItems,
        long activeItems,
        long lowStockItems,
        long outOfStockItems) {
}
