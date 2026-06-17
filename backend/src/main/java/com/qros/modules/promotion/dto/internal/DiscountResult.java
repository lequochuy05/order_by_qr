package com.qros.modules.promotion.dto.internal;

import com.qros.modules.promotion.model.enums.VoucherType;
import java.math.BigDecimal;

public record DiscountResult(
        Long voucherId,
        String voucherCode,
        VoucherType voucherType,
        BigDecimal subtotal,
        BigDecimal discountAmountSnapshot,
        BigDecimal discountPercentSnapshot,
        BigDecimal appliedDiscountAmount,
        BigDecimal finalAmount) {}
