package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CategoryResponse - Data transfer object for food and beverage categories.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Integer id;
    private String name;
    private String img;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private java.util.List<MenuItemResponse> menuItems;
}
