package com.qros.modules.analytics.dto.response;

public record TableSummaryResponse(
        Long totalTables,
        Long occupiedTables) {
}