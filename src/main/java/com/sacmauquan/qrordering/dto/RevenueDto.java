package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RevenueDto {
    private String bucket;      // "2025-08-01" (Ngày) | "2025-31" (Tuần) | "2025-08" (Tháng)
    private Double revenue;
    private Long orders;
}
