package com.qros.modules.menu.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * ComboItemRequest - Data transfer object for an individual item within a combo.
 */
public record ComboItemRequest(
        Long id,
        @NotNull(message = "Menu item ID is required") Long menuItemId,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1")
                Integer quantity) {}
