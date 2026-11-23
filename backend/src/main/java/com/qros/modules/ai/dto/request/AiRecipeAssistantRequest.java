package com.qros.modules.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

public record AiRecipeAssistantRequest(
        @NotNull(message = "Menu item id is required") Long menuItemId,

        @Valid List<DraftRecipeLine> currentItems,

        @Pattern(regexp = "^(vi|en)$", message = "Language must be vi or en")
        String language) {

    public record DraftRecipeLine(
            @NotNull(message = "Inventory item id is required") Long inventoryItemId,

            @NotNull(message = "Quantity required is required") @DecimalMin(value = "0.001", message = "Quantity required must be positive") BigDecimal quantityRequired) {}
}
