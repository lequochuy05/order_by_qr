package com.sacmauquan.qrordering.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class MenuItemRequest {
    @NotBlank(message = "Tên món ăn không được để trống")
    private String name;

    private String img;

    @NotNull(message = "Giá món ăn không được để trống")
    @Min(value = 0, message = "Giá món ăn không được âm")
    private BigDecimal price;

    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    private Boolean active;

    @Valid
    private List<ItemOptionRequest> itemOptions;

    @Data
    public static class ItemOptionRequest {
        private Long id;

        @NotBlank(message = "Tên lựa chọn không được để trống")
        private String name;

        @JsonProperty("isRequired")
        private boolean isRequired;

        @Min(value = 1, message = "Số lượng chọn tối đa phải ít nhất là 1")
        private int maxSelection;

        @Valid
        private List<ItemOptionValueRequest> optionValues;
    }

    @Data
    public static class ItemOptionValueRequest {
        private Long id;

        @NotBlank(message = "Tên giá trị không được để trống")
        private String name;

        @NotNull(message = "Giá thêm không được để trống")
        @Min(value = 0, message = "Giá thêm không được âm")
        private BigDecimal extraPrice;
    }
}
