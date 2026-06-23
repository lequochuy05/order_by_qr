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
