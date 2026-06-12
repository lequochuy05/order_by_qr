package com.qros.modules.menu.dto.response;

import com.qros.modules.menu.dto.summary.CategorySummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MenuItemResponse(

    Long id,
    String name,
    String description,
    String img,
    BigDecimal price,
    Boolean active,
    Boolean available,
    Integer displayOrder,
    CategorySummary category,
    List<ItemOptionResponse> itemOptions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt

) {
}