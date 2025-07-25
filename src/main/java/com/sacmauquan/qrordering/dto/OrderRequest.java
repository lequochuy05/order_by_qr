package com.sacmauquan.qrordering.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class OrderRequest {
    private Long tableId;
    private String status;
    private List<ItemRequest> items;

    @Getter
    @Setter
    public static class ItemRequest {
        private Long menuItemId;
        private int quantity;
        private String notes; 
    }
}
