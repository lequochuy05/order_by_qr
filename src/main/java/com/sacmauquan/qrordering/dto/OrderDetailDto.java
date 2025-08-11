package com.sacmauquan.qrordering.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderDetailDto {
    private Long id;
    private Instant paymentTime;
    private String employeeName;
    private Double totalAmount;
}
