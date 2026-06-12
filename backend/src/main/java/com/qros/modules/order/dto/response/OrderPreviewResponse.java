package com.qros.modules.order.dto.response;

import java.math.BigDecimal;

public record OrderPreviewResponse(
    BigDecimal subtotalItems,
    BigDecimal subtotalCombos,
    BigDecimal subtotalAmount,
    BigDecimal discountAmount,
    BigDecimal finalAmount,
    boolean voucherValid,
    String voucherMessage,
    BigDecimal discountPromotion) {
}
