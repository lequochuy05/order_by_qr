package com.qros.modules.promotion.dto.response;

import com.qros.modules.promotion.model.enums.VoucherType;
import com.qros.modules.promotion.model.enums.VoucherValidationStatus;
import java.math.BigDecimal;

public record VoucherValidateResponse(
        String code,
        VoucherValidationStatus status,
        VoucherType type,
        BigDecimal discountAmount,
        BigDecimal discountPercent,
        BigDecimal appliedDiscountAmount,
        BigDecimal finalAmount,
        boolean applicable) {}
