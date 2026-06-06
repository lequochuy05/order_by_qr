package com.qros.modules.order.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * OrderPreviewResponse - Data transfer object representing an order preview.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderPreviewResponse {
    private BigDecimal subtotalItems;
    private BigDecimal subtotalCombos;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal finalTotal;

    private boolean voucherValid;
    private String voucherMessage;
    private BigDecimal discountVoucher;
    private BigDecimal discountPromotion;

    private BigDecimal originalTotal;
}
