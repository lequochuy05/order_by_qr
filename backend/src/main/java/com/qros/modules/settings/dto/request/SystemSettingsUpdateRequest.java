package com.qros.modules.settings.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

public record SystemSettingsUpdateRequest(
        @NotBlank(message = "Restaurant name cannot be empty") @Size(max = 150, message = "Restaurant name cannot exceed 150 characters") String restaurantName,

        @Size(max = 30, message = "Restaurant phone cannot exceed 30 characters") String restaurantPhone,

        @Email(message = "Restaurant email is invalid") @Size(max = 150, message = "Restaurant email cannot exceed 150 characters") String restaurantEmail,

        @Size(max = 255, message = "Restaurant address cannot exceed 255 characters") String restaurantAddress,

        @Size(max = 500, message = "Logo URL cannot exceed 500 characters") String logoUrl,

        @Size(max = 100, message = "WiFi name cannot exceed 100 characters") String wifiName,

        @Size(max = 255, message = "WiFi password cannot exceed 255 characters") String wifiPassword,

        LocalTime openingTime,

        LocalTime closingTime,

        @Size(max = 10, message = "Currency cannot exceed 10 characters") String currency,

        @DecimalMin(value = "0.00", message = "Tax percent cannot be negative") @DecimalMax(value = "100.00", message = "Tax percent cannot exceed 100") BigDecimal taxPercent,

        @DecimalMin(value = "0.00", message = "Service charge percent cannot be negative") @DecimalMax(value = "100.00", message = "Service charge percent cannot exceed 100") BigDecimal serviceChargePercent,

        Boolean orderingEnabled,

        Boolean maintenanceMode) {

    @AssertTrue(message = "Closing time must be after opening time")
    public boolean isValidTimeRange() {
        return openingTime == null || closingTime == null || closingTime.isAfter(openingTime);
    }
}