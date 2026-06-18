package com.qros.modules.order.dto.response;

public record OrderMenuItemSummaryResponse(Long id, String name, OrderCategorySummaryResponse category) {}
