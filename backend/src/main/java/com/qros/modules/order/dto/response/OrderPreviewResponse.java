package com.qros.modules.order.dto.response;

import java.math.BigDecimal;

public record OrderPreviewResponse(
        BigDecimal subtotalItems,
        BigDecimal subtotalCombos,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal serviceChargePercent,
        BigDecimal serviceChargeAmount,
        BigDecimal taxPercent,
        BigDecimal taxAmount,
        BigDecimal finalAmount,
        boolean voucherValid,
        String voucherMessage,
        BigDecimal discountPromotion) {

    public OrderPreviewResponse(
            BigDecimal subtotalItems,
            BigDecimal subtotalCombos,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            boolean voucherValid,
            String voucherMessage,
            BigDecimal discountPromotion) {
        this(
                subtotalItems,
                subtotalCombos,
                subtotalAmount,
                discountAmount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                finalAmount,
                voucherValid,
                voucherMessage,
                discountPromotion);
    }
}
