package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.Voucher;
import lombok.*;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscountResult {
    private double finalTotal;
    private double discountValue;
    private Voucher voucher;   // null nếu không có voucher
}
