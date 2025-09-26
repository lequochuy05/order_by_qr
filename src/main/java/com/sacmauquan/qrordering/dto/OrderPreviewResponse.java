package com.sacmauquan.qrordering.dto;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class OrderPreviewResponse {
    private Double subtotalItems;
    private Double subtotalCombos;
    private Double finalTotal;

    private boolean voucherValid;
    private String voucherMessage;
    private Double discountVoucher;
    private Double discountPromotion;

    private Double originalTotal; // tổng trước khi áp dụng giảm giá
}
