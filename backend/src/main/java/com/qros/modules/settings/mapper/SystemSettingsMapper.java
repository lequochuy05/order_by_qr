package com.qros.modules.settings.mapper;

import com.qros.modules.settings.dto.request.SystemSettingsUpdateRequest;
import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.dto.response.SystemSettingsResponse;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingsMapper {

    public SystemSettingsResponse toResponse(SystemSettings settings) {
        return new SystemSettingsResponse(
                settings.getId(),
                settings.getVersion(),
                settings.getRestaurantName(),
                settings.getRestaurantPhone(),
                settings.getRestaurantEmail(),
                settings.getRestaurantAddress(),
                settings.getLogoUrl(),
                settings.getWifiName(),
                settings.getWifiPassword(),
                settings.getOpeningTime(),
                settings.getClosingTime(),
                settings.getCurrency(),
                settings.getTaxPercent(),
                settings.getServiceChargePercent(),
                settings.getOrderingEnabled(),
                settings.getMaintenanceMode(),
                settings.getCashPaymentEnabled(),
                settings.getOnlinePaymentEnabled(),
                settings.getPaymentQrExpiresInMinutes(),
                settings.getAutoConfirmOrders(),
                settings.getKitchenOverdueThresholdMinutes(),
                settings.getShowUnavailableItems(),
                settings.getShowRecommendations(),
                settings.getShowCombos(),
                settings.getBillTitle(),
                settings.getBillFooterMessage(),
                settings.getBillPaperSize(),
                settings.getShowWifiOnBill(),
                settings.getAutoPrintBill(),
                settings.getNewOrderNotificationEnabled(),
                settings.getPaymentNotificationEnabled(),
                settings.getKitchenOverdueNotificationEnabled());
    }

    public PublicSettingsResponse toPublicResponse(SystemSettings settings) {
        return new PublicSettingsResponse(
                settings.getRestaurantName(),
                settings.getRestaurantPhone(),
                settings.getRestaurantAddress(),
                settings.getLogoUrl(),
                settings.getWifiName(),
                settings.getOpeningTime(),
                settings.getClosingTime(),
                settings.getOrderingEnabled(),
                settings.getMaintenanceMode(),
                settings.getCashPaymentEnabled(),
                settings.getOnlinePaymentEnabled(),
                settings.getPaymentQrExpiresInMinutes(),
                settings.getAutoConfirmOrders(),
                settings.getKitchenOverdueThresholdMinutes(),
                settings.getShowUnavailableItems(),
                settings.getShowRecommendations(),
                settings.getShowCombos(),
                settings.getBillTitle(),
                settings.getBillFooterMessage(),
                settings.getBillPaperSize(),
                settings.getShowWifiOnBill(),
                settings.getAutoPrintBill(),
                settings.getNewOrderNotificationEnabled(),
                settings.getPaymentNotificationEnabled(),
                settings.getKitchenOverdueNotificationEnabled());
    }

    public void updateEntity(SystemSettings settings, SystemSettingsUpdateRequest request) {
        settings.setRestaurantName(normalizeRequiredText(request.restaurantName()));
        settings.setRestaurantPhone(normalizeNullableText(request.restaurantPhone()));
        settings.setRestaurantEmail(normalizeNullableText(request.restaurantEmail()));
        settings.setRestaurantAddress(normalizeNullableText(request.restaurantAddress()));
        settings.setLogoUrl(normalizeNullableText(request.logoUrl()));
        settings.setWifiName(normalizeNullableText(request.wifiName()));
        settings.setWifiPassword(normalizeNullableText(request.wifiPassword()));
        settings.setOpeningTime(request.openingTime());
        settings.setClosingTime(request.closingTime());
        settings.setCurrency(normalizeCurrency(request.currency()));
        settings.setTaxPercent(request.taxPercent());
        settings.setServiceChargePercent(request.serviceChargePercent());
        settings.setOrderingEnabled(request.orderingEnabled());
        settings.setMaintenanceMode(request.maintenanceMode());
        settings.setCashPaymentEnabled(request.cashPaymentEnabled());
        settings.setOnlinePaymentEnabled(request.onlinePaymentEnabled());
        settings.setPaymentQrExpiresInMinutes(request.paymentQrExpiresInMinutes());
        settings.setAutoConfirmOrders(request.autoConfirmOrders());
        settings.setKitchenOverdueThresholdMinutes(request.kitchenOverdueThresholdMinutes());
        settings.setShowUnavailableItems(request.showUnavailableItems());
        settings.setShowRecommendations(request.showRecommendations());
        settings.setShowCombos(request.showCombos());
        settings.setBillTitle(normalizeRequiredText(request.billTitle(), "Bill title is required"));
        settings.setBillFooterMessage(
                normalizeRequiredText(request.billFooterMessage(), "Bill footer message is required"));
        settings.setBillPaperSize(request.billPaperSize());
        settings.setShowWifiOnBill(request.showWifiOnBill());
        settings.setAutoPrintBill(request.autoPrintBill());
        settings.setNewOrderNotificationEnabled(request.newOrderNotificationEnabled());
        settings.setPaymentNotificationEnabled(request.paymentNotificationEnabled());
        settings.setKitchenOverdueNotificationEnabled(request.kitchenOverdueNotificationEnabled());
    }

    private String normalizeRequiredText(String value) {
        return normalizeRequiredText(value, "Restaurant name cannot be empty");
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Currency is required");
        }
        return currency.trim().toUpperCase();
    }
}
