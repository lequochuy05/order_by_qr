package com.qros.modules.menu.dto;

import lombok.Data;

/**
 * ComboItemRequest - Data transfer object for an individual item within a combo.
 */
@Data
public class ComboItemRequest {
    /**
     * ID of the existing combo item record (if updating).
     */
    private Long id;

    /**
     * ID of the menu item included in the combo.
     */
    private Long menuItemId;

    /**
     * Quantity of the menu item in this combo.
     */
    private Integer quantity;
}