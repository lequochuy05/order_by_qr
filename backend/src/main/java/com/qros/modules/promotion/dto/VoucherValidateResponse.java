package com.qros.modules.promotion.dto;

import java.math.BigDecimal;

/**
 * VoucherValidateResponse - Data transfer object for the result of a voucher
 * validation.
 */
public record VoucherValidateResponse(
        String code,
        String status, // ACTIVE | INACTIVE | EXPIRED | EXHAUSTED | NOT_FOUND
        BigDecimal discountValue,
        Double discountPercent,
        boolean applicable) {
}
