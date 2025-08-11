package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmpPerfDto {
    private Long userId;
    private String fullName;
    private Long orders;
    private Double revenue;
}
