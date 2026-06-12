package com.qros.modules.analytics.dto.response;

public record PopularItemForecastResponse(
        Long menuItemId,
        String itemName,
        String categoryName,
        Long estimatedQuantity) {
}