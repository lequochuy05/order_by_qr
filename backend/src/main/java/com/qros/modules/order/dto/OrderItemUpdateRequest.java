package com.qros.modules.order.dto;

import lombok.Data;

@Data
public class OrderItemUpdateRequest {
    private Integer quantity = 1;
    private String notes = "";
}
