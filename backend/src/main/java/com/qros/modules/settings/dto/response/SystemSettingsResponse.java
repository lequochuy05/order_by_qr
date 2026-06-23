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
        Boolean maintenanceMode,
        Boolean cashPaymentEnabled,
        Boolean onlinePaymentEnabled,
        Integer paymentQrExpiresInMinutes,
        Boolean autoConfirmOrders,
        Integer kitchenOverdueThresholdMinutes,
        Boolean showUnavailableItems,
        Boolean showRecommendations,
        Boolean showCombos,
        String billTitle,
        String billFooterMessage,
        String billPaperSize,
        Boolean showWifiOnBill,
        Boolean autoPrintBill,
        Boolean newOrderNotificationEnabled,
        Boolean paymentNotificationEnabled,
        Boolean kitchenOverdueNotificationEnabled) {}
