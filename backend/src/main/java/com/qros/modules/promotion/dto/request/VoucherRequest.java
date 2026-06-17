package com.qros.modules.promotion.dto.request;

import com.qros.modules.promotion.model.enums.VoucherType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherRequest(
        @NotBlank(message = "Voucher code cannot be empty")
                @Size(max = 50, message = "Voucher code cannot exceed 50 characters")
                @Pattern(
                        regexp = "^[A-Za-z0-9_-]+$",
                        message = "Voucher code can only contain letters, numbers, underscore and hyphen")
                String code,
        @NotNull(message = "Voucher type cannot be empty") VoucherType type,
        @DecimalMin(value = "0.01", message = "Discount amount must be greater than 0") BigDecimal discountAmount,
        @DecimalMin(value = "0.01", message = "Discount percentage must be greater than 0")
                @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
                BigDecimal discountPercent,
        Boolean active,
        @PositiveOrZero(message = "Usage limit cannot be negative") Integer usageLimit,
        @NotNull(message = "Valid from date cannot be empty") LocalDateTime validFrom,
        @NotNull(message = "Valid to date cannot be empty") LocalDateTime validTo) {

    @AssertTrue(message = "Valid to date must be after valid from date")
    public boolean isValidDateRange() {
        return validFrom == null || validTo == null || validTo.isAfter(validFrom);
    }

    @AssertTrue(message = "FIXED_AMOUNT voucher must have discountAmount only")
    public boolean isValidFixedAmountVoucher() {
        if (type != VoucherType.FIXED_AMOUNT) {
            return true;
        }

        return discountAmount != null && discountPercent == null;
    }

    @AssertTrue(message = "PERCENTAGE voucher must have discountPercent only")
    public boolean isValidPercentageVoucher() {
        if (type != VoucherType.PERCENTAGE) {
            return true;
        }

        return discountPercent != null && discountAmount == null;
    }
}
