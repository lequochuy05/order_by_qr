package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MenuItemResponse - DTO trả về thông tin món ăn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {
    private Long id;
    private String name;
    private String img;
    private BigDecimal price;
    private Boolean active;
    private CategorySummary category;
    private List<ItemOptionResponse> itemOptions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Integer id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemOptionResponse {
        private Long id;
        private String name;
        @JsonProperty("isRequired")
        private boolean isRequired;
        private int maxSelection;
        private List<ItemOptionValueResponse> optionValues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemOptionValueResponse {
        private Long id;
        private String name;
        private BigDecimal extraPrice;
    }
}
