package com.sacmauquan.qrordering.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * OrderRequest - Data transfer object containing ordering information from a
 * customer or staff.
 */
@Getter
@Setter
public class OrderRequest {

    /**
     * Unique code from the table's QR code (used for customer scans).
     */
    private String tableCode;

    /**
     * ID of the dining table (used for staff direct selection).
     */
    private Long tableId;

    /**
     * Desired status for the order (e.g., PENDING, COMPLETED).
     */
    private String status;

    /**
     * Optional voucher code applied to the order for discounts.
     */
    private String voucherCode;

    /**
     * List of combo packages included in the order.
     */
    private List<OrderComboRequest> combos;

    /**
     * List of individual menu items included in the order.
     */
    private List<OrderItemRequest> items;

    /**
     * DTO for a combo package within an order.
     */
    @Getter
    @Setter
    public static class OrderComboRequest {
        @NotNull(message = "Combo ID cannot be empty")
        private Long comboId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        /**
         * Optional notes for the combo (e.g., specific instructions).
         */
        private String notes;
    }

    /**
     * DTO for an individual menu item within an order.
     */
    @Getter
    @Setter
    public static class OrderItemRequest {
        @NotNull(message = "Menu item ID cannot be empty")
        private Long menuItemId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        /**
         * Optional notes for the item (e.g., "no ice", "less sugar").
         */
        private String notes;

        /**
         * List of IDs for selected toppings or item options.
         */
        private List<Long> selectedOptionValueIds;
    }
}
