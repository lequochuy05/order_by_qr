package com.qros.modules.ai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AiInvoiceOcrResponse(
        String language,
        String supplierName,
        String invoiceDate,
        String invoiceNumber,
        BigDecimal totalAmount,
        List<ExtractedInvoiceLine> lines,
        List<String> warnings,
        LocalDateTime generatedAt,
        boolean aiGenerated) {

    public AiInvoiceOcrResponse {
        lines = lines == null ? List.of() : List.copyOf(lines);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public record ExtractedInvoiceLine(
            String rawName,
            BigDecimal invoiceQuantity,
            String invoiceUnit,
            BigDecimal invoiceUnitPrice,
            BigDecimal lineTotal,
            Long matchedInventoryItemId,
            String matchedInventoryName,
            String matchedInventoryUnit,
            BigDecimal currentUnitCost,
            BigDecimal convertedQuantity,
            BigDecimal convertedUnitCost,
            boolean unitConverted,
            boolean unitMismatch,
            String matchStatus,
            String confidence,
            String conversionNote) {}
}
