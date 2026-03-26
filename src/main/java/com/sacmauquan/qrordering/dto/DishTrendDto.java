package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DishTrendDto {
    private String date;
    private Long totalQuantity;
}
