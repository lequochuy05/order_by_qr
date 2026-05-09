package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.Voucher;
import java.math.BigDecimal;

/**
 * DiscountResult - Kết quả tính toán giảm giá cho đơn hàng.
 */
public record DiscountResult(
    BigDecimal finalTotal,
    BigDecimal discountValue,
    Voucher voucher
) {}
