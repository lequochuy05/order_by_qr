package com.sacmauquan.qrordering.dto;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

    /**
     * Total amount to be paid. Must be a positive value.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
}
