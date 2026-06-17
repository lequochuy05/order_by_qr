package com.qros.modules.menu.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record MenuItemRequest(
        @NotBlank(message = "Item name cannot be empty")
                @Size(max = 100, message = "Item name cannot exceed 100 characters")
                String name,
        @Size(max = 500, message = "Description cannot exceed 500 characters") String description,
        @Size(max = 500, message = "Image URL cannot exceed 500 characters") String img,
        @NotNull(message = "Price cannot be empty") @DecimalMin(value = "0.0", message = "Price cannot be negative")
                BigDecimal price,
        @NotNull(message = "Category ID cannot be empty") Long categoryId,
        Boolean active,
        Boolean available,
        Integer displayOrder,
        @Valid List<ItemOptionRequest> itemOptions) {}
