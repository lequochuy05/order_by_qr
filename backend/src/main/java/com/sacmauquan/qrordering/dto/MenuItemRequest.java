package com.sacmauquan.qrordering.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MenuItemRequest - Data transfer object for creating or updating a menu item.
 */
@Data
public class MenuItemRequest {
    /**
     * Name of the dish or beverage.
     */
    @NotBlank(message = "Item name cannot be empty")
    private String name;

    /**
     * URL or identifier of the item's representative image.
     */
    private String img;

    /**
     * Base price of the menu item.
     */
    @NotNull(message = "Price cannot be empty")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    /**
     * ID of the category this item belongs to.
     */
    @NotNull(message = "Category ID cannot be empty")
    private Integer categoryId;

    /**
     * Status to determine if the item is available for sale.
     */
    private Boolean active;

    /**
     * List of customizable options (e.g., Size, Toppings).
     */
    @Valid
    private List<ItemOptionRequest> itemOptions;

    /**
     * Request DTO for an item option.
     */
    @Data
    public static class ItemOptionRequest {
        private Long id;

        @NotBlank(message = "Option name cannot be empty")
        private String name;

        @JsonProperty("isRequired")
        private boolean isRequired;

        @Min(value = 1, message = "Max selection must be at least 1")
        private int maxSelection;

        @Valid
        private List<ItemOptionValueRequest> optionValues;
    }

    /**
     * Request DTO for a specific value within an item option.
     */
    @Data
    public static class ItemOptionValueRequest {
        private Long id;

        @NotBlank(message = "Option value name cannot be empty")
        private String name;

        @NotNull(message = "Extra price cannot be empty")
        @Min(value = 0, message = "Extra price cannot be negative")
        private BigDecimal extraPrice;
    }
}
