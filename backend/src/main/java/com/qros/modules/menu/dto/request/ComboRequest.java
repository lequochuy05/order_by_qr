package com.qros.modules.menu.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * ComboRequest - Data transfer object for creating or updating a combo package.
 */
public record ComboRequest(
        @NotBlank(message = "Combo name cannot be empty")
                @Size(max = 100, message = "Combo name cannot exceed 100 characters")
                String name,
        @Size(max = 500, message = "Description cannot exceed 500 characters") String description,
        @NotNull(message = "Price cannot be empty") @DecimalMin(value = "0.0", message = "Price cannot be negative")
                BigDecimal price,
        Boolean active,
        Boolean available,
        Integer displayOrder,
        @Valid @NotEmpty(message = "Combo must contain at least one item") List<ComboItemRequest> items,
        Long version) {}
