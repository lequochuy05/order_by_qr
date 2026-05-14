package com.sacmauquan.qrordering.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * PayosCreateRequest - Data transfer object for initiating a PayOS payment link.
 */
@Data
public class PayosCreateRequest {
    /**
     * ID of the order to create a payment link for.
     */
    @NotNull(message = "Order ID is required")
    private Long orderId;
}
