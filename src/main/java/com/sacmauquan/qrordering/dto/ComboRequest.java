package com.sacmauquan.qrordering.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * ComboRequest - Data transfer object for creating or updating a combo package.
 */
@Data
public class ComboRequest {
    /**
     * Name of the combo package.
     */
    @NotBlank(message = "Combo name cannot be empty")
    private String name;

    /**
     * Total price of the combo.
     */
    @NotNull(message = "Price cannot be empty")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    /**
     * Status to determine if the combo is available for sale.
     */
    private Boolean active;

    /**
     * List of individual items included in this combo.
     */
    private List<ComboItemRequest> items;
}
