package com.qros.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecipeUpdateRequest(
        @NotNull(message = "Recipe items cannot be null") @Size(max = 100, message = "Recipe items cannot exceed 100")
                List<@Valid RecipeItemRequest> items) {}
