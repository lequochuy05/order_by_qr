package com.qros.modules.settings.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

public record SystemSettingsUpdateRequest(
        @NotBlank(message = "Restaurant name cannot be empty")
                @Size(max = 150, message = "Restaurant name cannot exceed 150 characters")
                String restaurantName,
        @Size(max = 30, message = "Restaurant phone cannot exceed 30 characters")
                @Pattern(
                        regexp = "^(?:[0-9+()\\-\\s]{6,30})?$",
                        message = "Restaurant phone contains invalid characters")
                String restaurantPhone,
        @Email(message = "Restaurant email is invalid")
                @Size(max = 150, message = "Restaurant email cannot exceed 150 characters")
                String restaurantEmail,
        @Size(max = 255, message = "Restaurant address cannot exceed 255 characters") String restaurantAddress,
        @Size(max = 500, message = "Logo URL cannot exceed 500 characters")
                @Pattern(regexp = "(?i)^(?:https?://\\S+)?$", message = "Logo URL must use HTTP or HTTPS")
                String logoUrl,
        @Size(max = 100, message = "WiFi name cannot exceed 100 characters") String wifiName,
        @Size(max = 255, message = "WiFi password cannot exceed 255 characters") String wifiPassword,
        LocalTime openingTime,
        LocalTime closingTime,
        @NotBlank(message = "Currency is required")
                @Pattern(regexp = "(?i)^[A-Z]{3}$", message = "Currency must use ISO-4217 format")
                String currency,
        @NotNull(message = "Tax percent is required") @DecimalMin(value = "0.00", message = "Tax percent cannot be negative")
                @DecimalMax(value = "100.00", message = "Tax percent cannot exceed 100")
                BigDecimal taxPercent,
        @NotNull(message = "Service charge percent is required") @DecimalMin(value = "0.00", message = "Service charge percent cannot be negative")
                @DecimalMax(value = "100.00", message = "Service charge percent cannot exceed 100")
                BigDecimal serviceChargePercent,
        @NotNull(message = "Ordering enabled is required") Boolean orderingEnabled,
        @NotNull(message = "Maintenance mode is required") Boolean maintenanceMode,
        Long version) {

    @AssertTrue(message = "Opening and closing time cannot be the same")
    public boolean isValidTimeRange() {
        return openingTime == null || closingTime == null || !closingTime.equals(openingTime);
    }
}
