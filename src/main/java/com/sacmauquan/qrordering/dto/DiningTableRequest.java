package com.sacmauquan.qrordering.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiningTableRequest {
    private String qrCodeUrl;
    private String tableNumber;
    private String status;
    private int capacity;
}
