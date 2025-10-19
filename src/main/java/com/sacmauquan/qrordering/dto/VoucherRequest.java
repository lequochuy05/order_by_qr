package com.sacmauquan.qrordering.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
public class VoucherRequest {
    private String code;
    private Double discountAmount;
    private Double discountPercent;
    private Boolean active;
    private Integer usageLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
