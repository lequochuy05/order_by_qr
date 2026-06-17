package com.qros.modules.table.dto.response;

public record TableSessionStartResponse(
        String tableCode,
        String tableNumber,
        String tableStatus,
        Long sessionId,
        String sessionToken,
        String sessionStatus,
        boolean canOrder) {
}
