package com.qros.modules.inventory.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BatchStockInResponse(
        int importedCount, BigDecimal totalEstimatedValue, List<InventoryItemResponse> items) {

    public BatchStockInResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
