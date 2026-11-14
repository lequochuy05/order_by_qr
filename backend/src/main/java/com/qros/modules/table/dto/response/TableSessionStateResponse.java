package com.qros.modules.table.dto.response;

import java.util.List;

public record TableSessionStateResponse(
        String tableCode,
        String tableNumber,
        String tableStatus,
        boolean hasOpenSession,
        boolean canStartSession,
        boolean canOrder,
        Long sessionId,
        Long primaryTableId,
        List<Long> tableIds,
        List<String> tableNumbers,
        String displayName,
        Integer guestCount,
        Integer totalCapacity,
        boolean merged) {}
