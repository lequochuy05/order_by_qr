package com.sacmauquan.qrordering.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class OrderRequest {
    private Long tableId;
    private String status;

    // Mã voucher (nếu có)
    private String voucherCode;

    //Combo ID (nếu có)
    private List<Long> comboIds;

    private List<ItemRequest> items;

    @Getter
    @Setter
    public static class ItemRequest {
        private Long menuItemId;
        private int quantity;
        private String notes; 
    }
}
