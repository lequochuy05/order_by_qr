package com.sacmauquan.qrordering.dto;

import com.sacmauquan.qrordering.model.Voucher;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VoucherRequest - Data transfer object for creating or updating a discount voucher.
 */
@Getter
@Setter
public class VoucherRequest {
    /**
     * Unique voucher code.
     */
    @NotBlank(message = "Voucher code cannot be empty")
    private String code;

    /**
     * Type of the voucher (e.g., FIXED_AMOUNT, PERCENTAGE).
     */
    @NotNull(message = "Voucher type cannot be empty")
    private Voucher.VoucherType type;

    /**
     * Absolute amount to be deducted if type is FIXED_AMOUNT.
     */
    @Min(value = 0, message = "Discount amount cannot be negative")
    private BigDecimal discountAmount;

    /**
     * Percentage to be deducted if type is PERCENTAGE.
     */
    @Min(value = 0, message = "Discount percentage cannot be negative")
    private Double discountPercent;

    /**
     * Status to determine if the voucher is active and usable.
     */
    private Boolean active;

    /**
     * Maximum number of times this voucher can be used.
     */
    private Integer usageLimit;

    /**
     * Starting date and time for the voucher's validity.
     */
    @NotNull(message = "Valid from date cannot be empty")
    private LocalDateTime validFrom;

    /**
     * Ending date and time for the voucher's validity.
     */
    @NotNull(message = "Valid to date cannot be empty")
    private LocalDateTime validTo;
}
