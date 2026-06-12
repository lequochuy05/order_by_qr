package com.qros.modules.promotion.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record PromotionRequest(
        @NotBlank(message = "Promotion name cannot be empty") @Size(max = 150, message = "Promotion name cannot exceed 150 characters") String name,

        @NotNull(message = "Discount percentage cannot be empty") @DecimalMin(value = "0.01", message = "Discount percentage must be greater than 0") @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100") BigDecimal discountPercent,

        @NotNull(message = "Start time cannot be empty") LocalTime startTime,

        @NotNull(message = "End time cannot be empty") LocalTime endTime,

        Set<DayOfWeek> daysOfWeek,

        Boolean active) {

    @AssertTrue(message = "End time must be after start time")
    public boolean isValidTimeRange() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}