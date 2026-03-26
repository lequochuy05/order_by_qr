package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopDishDto {
    private Long menuItemId;
    private String name;
    private String categoryName;
    private String img;
    private Long totalQuantity;
    private Double totalRevenue;
}
