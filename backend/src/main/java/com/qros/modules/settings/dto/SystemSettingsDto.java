package com.qros.modules.settings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingsDto {
    private Integer id;

    @NotBlank(message = "Restaurant name cannot be empty")
    @Size(max = 100, message = "Restaurant name cannot exceed 100 characters")
    private String restaurantName;

    private String restaurantAddress;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String restaurantPhone;

    private String restaurantLogoUrl;

    @DecimalMin(value = "0.0", message = "VAT rate cannot be negative")
    private BigDecimal vatRate;

    @Size(max = 50, message = "WiFi SSID cannot exceed 50 characters")
    private String wifiSsid;

    @Size(max = 50, message = "WiFi password cannot exceed 50 characters")
    private String wifiPassword;

    private Boolean autoApproveOrders;
    private Boolean enableAiAssistant;
    private Boolean enablePayos;
    private Boolean enableCash;
    private LocalDateTime updatedAt;
}
