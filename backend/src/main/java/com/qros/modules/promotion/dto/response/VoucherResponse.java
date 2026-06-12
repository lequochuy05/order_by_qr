package com.qros.modules.promotion.dto.response;

import com.qros.modules.promotion.model.enums.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherResponse(
        Long id,
        String code,
        VoucherType type,
        BigDecimal discountAmount,
        BigDecimal discountPercent,
        Integer usageLimit,
        Integer usedCount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Boolean active) {
}