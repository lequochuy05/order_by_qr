package com.qros.modules.settings.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
        @NotNull(message = "Cash payment setting is required") Boolean cashPaymentEnabled,
        @NotNull(message = "Online payment setting is required") Boolean onlinePaymentEnabled,
        @NotNull(message = "Payment QR expiry is required") @Min(value = 1, message = "Payment QR expiry must be at least 1 minute")
                @Max(value = 120, message = "Payment QR expiry cannot exceed 120 minutes")
                Integer paymentQrExpiresInMinutes,
        @NotNull(message = "Auto confirm order setting is required") Boolean autoConfirmOrders,
        @NotNull(message = "Kitchen overdue threshold is required") @Min(value = 1, message = "Kitchen overdue threshold must be at least 1 minute")
                @Max(value = 240, message = "Kitchen overdue threshold cannot exceed 240 minutes")
                Integer kitchenOverdueThresholdMinutes,
        @NotNull(message = "Unavailable item display setting is required") Boolean showUnavailableItems,
        @NotNull(message = "Recommendation display setting is required") Boolean showRecommendations,
        @NotNull(message = "Combo display setting is required") Boolean showCombos,
        @NotBlank(message = "Bill title is required")
                @Size(max = 100, message = "Bill title cannot exceed 100 characters")
                String billTitle,
        @NotBlank(message = "Bill footer message is required")
                @Size(max = 255, message = "Bill footer message cannot exceed 255 characters")
                String billFooterMessage,
        @NotBlank(message = "Bill paper size is required")
                @Pattern(regexp = "^(58|80)$", message = "Bill paper size must be 58 or 80")
                String billPaperSize,
        @NotNull(message = "Show WiFi on bill setting is required") Boolean showWifiOnBill,
        @NotNull(message = "Auto print bill setting is required") Boolean autoPrintBill,
        @NotNull(message = "New order notification setting is required") Boolean newOrderNotificationEnabled,
        @NotNull(message = "Payment notification setting is required") Boolean paymentNotificationEnabled,
        @NotNull(message = "Kitchen overdue notification setting is required") Boolean kitchenOverdueNotificationEnabled,
        Long version) {

    @AssertTrue(message = "Opening and closing time cannot be the same")
    public boolean isValidTimeRange() {
        return openingTime == null || closingTime == null || !closingTime.equals(openingTime);
    }

    @AssertTrue(message = "At least one payment method must be enabled")
    public boolean isPaymentMethodEnabled() {
        return Boolean.TRUE.equals(cashPaymentEnabled) || Boolean.TRUE.equals(onlinePaymentEnabled);
    }
}
