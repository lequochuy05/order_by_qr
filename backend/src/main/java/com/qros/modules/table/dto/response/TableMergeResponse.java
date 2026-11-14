package com.qros.modules.table.dto.response;

import java.util.List;

public record TableMergeResponse(
        Long sessionId,
        Long primaryTableId,
        String primaryTableNumber,
        List<Long> tableIds,
        List<String> tableNumbers,
        String displayName,
        Integer guestCount,
        Integer totalCapacity,
        String status,
        Long activeOrderId) {}
