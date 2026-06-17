package com.qros.modules.promotion.dto.internal;

import com.qros.modules.promotion.model.Voucher;
import java.math.BigDecimal;

public record VoucherPaymentResult(Voucher voucher, DiscountResult discountResult) {

    public BigDecimal appliedDiscountAmount() {
        return discountResult != null && discountResult.appliedDiscountAmount() != null
                ? discountResult.appliedDiscountAmount()
                : BigDecimal.ZERO;
    }

    public BigDecimal finalAmount() {
        return discountResult != null && discountResult.finalAmount() != null
                ? discountResult.finalAmount()
                : BigDecimal.ZERO;
    }

    public String voucherCode() {
        return discountResult != null ? discountResult.voucherCode() : null;
    }
}
