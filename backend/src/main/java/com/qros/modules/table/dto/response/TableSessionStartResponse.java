package com.qros.modules.table.dto.response;

import java.util.List;

public record TableSessionStartResponse(
        String tableCode,
        String tableNumber,
        String tableStatus,
        Long sessionId,
        String sessionToken,
        String sessionStatus,
        boolean canOrder,
        Long primaryTableId,
        List<Long> tableIds,
        List<String> tableNumbers,
        String displayName,
        Integer guestCount,
        Integer totalCapacity,
        boolean merged) {}
