package com.qros.modules.promotion.dto;

import com.qros.modules.promotion.model.Voucher;
import java.math.BigDecimal;

/**
 * DiscountResult - Data transfer object representing the result of a discount
 * calculation.
 */
public record DiscountResult(
        BigDecimal finalTotal,
        BigDecimal discountValue,
        Voucher voucher) {
}
