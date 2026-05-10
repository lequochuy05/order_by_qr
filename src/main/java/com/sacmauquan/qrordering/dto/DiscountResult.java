package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.Voucher;
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
