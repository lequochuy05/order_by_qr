package com.sacmauquan.qrordering.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiningTableResponse {
     private Long id;
   
    private String qrCodeUrl;
     private String tableNumber;
    private String status;
    private int capacity;
}
