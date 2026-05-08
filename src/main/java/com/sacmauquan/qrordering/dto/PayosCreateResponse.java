package com.sacmauquan.qrordering.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PayosCreateResponse {
    private Long transactionId;
    private String checkoutUrl;
    private String qrCode;
    private LocalDateTime createdAt;
}
