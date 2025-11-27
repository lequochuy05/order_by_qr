package com.sacmauquan.qrordering.dto;


import java.util.List;

import lombok.Data;

@Data
public class ComboRequest {
    private String name;
    private Double price;
    private Boolean active;
    private List<ComboItemRequest> items;
}

