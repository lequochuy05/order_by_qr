package com.sacmauquan.qrordering.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * OrderRequest - Chứa thông tin đặt món từ khách hàng hoặc nhân viên.
 */
@Getter
@Setter
public class OrderRequest {
    
    private String tableCode; // Dùng cho khách quét QR
    private Long tableId;     // Dùng cho nhân viên chọn bàn trực tiếp
    
    private String status;      // Mặc định là PENDING nếu từ khách
    private String voucherCode; // Mã giảm giá áp dụng

    private List<OrderComboRequest> combos;
    private List<OrderItemRequest> items;

    @Getter
    @Setter
    public static class OrderComboRequest {
        @NotNull(message = "ID combo không được để trống")
        private Long comboId;
        
        @Min(value = 1, message = "Số lượng phải ít nhất là 1")
        private int quantity;
        
        private String notes;
    }

    @Getter
    @Setter
    public static class OrderItemRequest {
        @NotNull(message = "ID món ăn không được để trống")
        private Long menuItemId;
        
        @Min(value = 1, message = "Số lượng phải ít nhất là 1")
        private int quantity;
        
        private String notes;
        
        // Danh sách ID của các topping/tùy chọn được chọn
        private List<Long> selectedOptionValueIds;
    }
}
