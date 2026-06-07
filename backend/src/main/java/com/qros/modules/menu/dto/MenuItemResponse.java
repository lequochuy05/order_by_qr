package com.qros.modules.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MenuItemResponse - Data transfer object representing a menu item and its related options.
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

    /**
     * Brief summary of a category.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Integer id;
        private String name;
    }

    /**
     * Response DTO for a menu item option.
     */
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

    /**
     * Response DTO for a specific value within a menu item option.
     */
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
