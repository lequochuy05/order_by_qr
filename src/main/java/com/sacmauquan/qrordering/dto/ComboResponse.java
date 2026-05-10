package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * ComboResponse - Data transfer object for combo packages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Boolean active;
    private List<ComboItemResponse> items;

    /**
     * ComboItemResponse - Summary of a menu item within a combo.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboItemResponse {
        private Long id;
        private Long menuItemId;
        private String menuItemName;
        private String menuItemImg;
        private Integer quantity;
    }
}
