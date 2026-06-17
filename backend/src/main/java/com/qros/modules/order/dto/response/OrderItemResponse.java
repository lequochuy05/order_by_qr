package com.qros.modules.order.dto.response;

import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderItemType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderItemResponse(
        Long id,
        Long batchId,
        OrderMenuItemSummaryResponse menuItem,
        OrderComboSummaryResponse combo,
        String itemNameSnapshot,
        OrderItemType itemType,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String notes,
        boolean prepared,
        OrderItemStatus status,
        List<OrderItemOptionResponse> options,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
