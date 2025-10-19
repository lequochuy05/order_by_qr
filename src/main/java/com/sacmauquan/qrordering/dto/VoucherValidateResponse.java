package com.sacmauquan.qrordering.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class VoucherValidateResponse {
    private String code;
    private String status;          // ACTIVE | INACTIVE | EXPIRED | EXHAUSTED | NOT_FOUND
    private Double discountAmount;  // số tiền giảm quy đổi theo total (nếu là % thì tính ra tiền)
    private Double discountPercent; // nếu là %
    private boolean applicable;  
}
