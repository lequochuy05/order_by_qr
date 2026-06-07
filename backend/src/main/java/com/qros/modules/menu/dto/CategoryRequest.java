package com.qros.modules.menu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * CategoryRequest - DTO for creating and updating categories.
 */
@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
    
    private Boolean active;
    
    private String img;
}
