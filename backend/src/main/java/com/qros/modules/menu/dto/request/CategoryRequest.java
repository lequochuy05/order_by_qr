package com.qros.modules.menu.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CategoryRequest - DTO for creating and updating categories.
 */
public record CategoryRequest(
        @NotBlank(message = "Category name cannot be empty")
                @Size(max = 50, message = "Category name cannot exceed 50 characters")
                String name,
        @Size(max = 500, message = "Image URL cannot exceed 500 characters") String img,
        @Size(max = 500, message = "Description cannot exceed 500 characters") String description,
        Boolean active,
        @Min(value = 0, message = "Display order cannot be negative") Integer displayOrder,
        Long version) {}
