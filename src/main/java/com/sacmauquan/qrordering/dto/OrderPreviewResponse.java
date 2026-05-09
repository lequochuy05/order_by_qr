package com.sacmauquan.qrordering.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * OrderPreviewResponse - Dữ liệu xem trước đơn hàng.
 */
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class OrderPreviewResponse {
    private BigDecimal subtotalItems;
    private BigDecimal subtotalCombos;
    private BigDecimal finalTotal;

    private boolean voucherValid;
    private String voucherMessage;
    private BigDecimal discountVoucher;
    private BigDecimal discountPromotion;

    private BigDecimal originalTotal; // Tổng trước khi áp dụng giảm giá
}
