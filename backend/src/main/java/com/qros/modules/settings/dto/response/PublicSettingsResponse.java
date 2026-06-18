package com.qros.modules.settings.dto.response;

import java.time.LocalTime;

public record PublicSettingsResponse(
        String restaurantName,
        String restaurantPhone,
        String restaurantAddress,
        String logoUrl,
        String wifiName,
        LocalTime openingTime,
        LocalTime closingTime,
        Boolean orderingEnabled,
        Boolean maintenanceMode) {}
