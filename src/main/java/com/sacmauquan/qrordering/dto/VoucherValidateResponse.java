package com.sacmauquan.qrordering.dto;

import java.math.BigDecimal;

/**
 * VoucherValidateResponse - Dữ liệu trả về khi khách hàng kiểm tra mã giảm giá.
 * Sử dụng Record để đảm bảo tính bất biến.
 */
public record VoucherValidateResponse(
    String code,
    String status,          // ACTIVE | INACTIVE | EXPIRED | EXHAUSTED | NOT_FOUND
    BigDecimal discountValue, // Số tiền được giảm thực tế
    Double discountPercent,   // Phần trăm giảm (nếu có)
    boolean applicable
) {}
