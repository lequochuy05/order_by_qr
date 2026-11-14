package com.qros.modules.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TableBoardResponse(List<TableItem> tables, List<ActiveOrder> activeOrders) {

    public record TableItem(
            Long id,
            String tableNumber,
            String tableCode,
            String status,
            int capacity,
            String qrCodeUrl,
            boolean hasOpenSession,
            String sessionStatus,
            LocalDateTime sessionOpenedAt,
            LocalDateTime sessionLastActivityAt,
            Long sessionId,
            Long primaryTableId,
            boolean merged,
            List<Long> memberTableIds,
            List<String> memberTableNumbers,
            String displayName,
            Integer guestCount,
            Integer totalCapacity,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    public record ActiveOrder(
            Long id,
            String status,
            BigDecimal finalAmount,
            Long tableId,
            String tableNumber,
            Long sessionId,
            Long primaryTableId,
            List<Long> memberTableIds,
            List<String> memberTableNumbers,
            String displayName,
            LocalDateTime createdAt) {}
}
