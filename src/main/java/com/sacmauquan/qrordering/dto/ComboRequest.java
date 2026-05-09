package com.sacmauquan.qrordering.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
public class ComboRequest {
    @NotBlank(message = "Tên combo không được để trống")
    private String name;

    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá không được âm")
    private BigDecimal price;

    private Boolean active;

    private List<ComboItemRequest> items;
}
