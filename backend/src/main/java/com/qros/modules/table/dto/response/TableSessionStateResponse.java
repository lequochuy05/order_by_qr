package com.qros.modules.table.dto.response;

public record TableSessionStateResponse(
        String tableCode,
        String tableNumber,
        String tableStatus,
        boolean hasOpenSession,
        boolean canStartSession,
        boolean canOrder) {}
