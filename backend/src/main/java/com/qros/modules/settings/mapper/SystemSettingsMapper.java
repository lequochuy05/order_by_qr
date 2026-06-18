package com.qros.modules.settings.mapper;

import com.qros.modules.settings.dto.request.SystemSettingsUpdateRequest;
import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.dto.response.SystemSettingsResponse;
import com.qros.modules.settings.model.SystemSettings;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingsMapper {

    public SystemSettingsResponse toResponse(SystemSettings settings) {
        return new SystemSettingsResponse(
                settings.getId(),
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
                settings.getMaintenanceMode());
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
                settings.getMaintenanceMode());
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
        settings.setTaxPercent(defaultMoneyPercent(request.taxPercent()));
        settings.setServiceChargePercent(defaultMoneyPercent(request.serviceChargePercent()));
        settings.setOrderingEnabled(request.orderingEnabled() != null ? request.orderingEnabled() : true);
        settings.setMaintenanceMode(request.maintenanceMode() != null ? request.maintenanceMode() : false);
    }

    public SystemSettings defaultSettings() {
        return SystemSettings.builder()
                .id(1L)
                .restaurantName("QROS Restaurant")
                .currency("VND")
                .taxPercent(BigDecimal.ZERO)
                .serviceChargePercent(BigDecimal.ZERO)
                .orderingEnabled(true)
                .maintenanceMode(false)
                .build();
    }

    private String normalizeRequiredText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "VND";
        }

        return currency.trim().toUpperCase();
    }

    private BigDecimal defaultMoneyPercent(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
