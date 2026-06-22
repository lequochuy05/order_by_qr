package com.qros.modules.settings.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;

public record SystemSettingsResponse(
        Long id,
        Long version,
        String restaurantName,
        String restaurantPhone,
        String restaurantEmail,
        String restaurantAddress,
        String logoUrl,
        String wifiName,
        String wifiPassword,
        LocalTime openingTime,
        LocalTime closingTime,
        String currency,
        BigDecimal taxPercent,
        BigDecimal serviceChargePercent,
        Boolean orderingEnabled,
        Boolean maintenanceMode) {}
