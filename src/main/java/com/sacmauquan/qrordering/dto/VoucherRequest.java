package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.Voucher;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotNull(message = "Loại voucher không được để trống")
    private Voucher.VoucherType type;

    @Min(value = 0, message = "Số tiền giảm không được âm")
    private BigDecimal discountAmount;

    @Min(value = 0, message = "Phần trăm giảm không được âm")
    private Double discountPercent;

    private Boolean active;

    @Min(value = 1, message = "Giới hạn sử dụng phải ít nhất là 1")
    private Integer usageLimit;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime validFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime validTo;
}
