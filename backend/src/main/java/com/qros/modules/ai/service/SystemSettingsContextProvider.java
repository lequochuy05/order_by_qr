package com.qros.modules.ai.service;

import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.service.SystemSettingsService;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemSettingsContextProvider {

    private final SystemSettingsService settingsService;

    public String buildSettingsContext() {
        PublicSettingsResponse settings = settingsService.getPublicSettings();

        StringBuilder sb = new StringBuilder();

        sb.append("THÔNG TIN NHÀ HÀNG:\n");
        appendField(sb, "Tên quán", settings.restaurantName());
        appendField(sb, "Địa chỉ", settings.restaurantAddress());
        appendField(sb, "Số điện thoại", settings.restaurantPhone());

        if (settings.openingTime() != null && settings.closingTime() != null) {
            sb.append("  - Giờ mở cửa: ")
                    .append(formatTime(settings.openingTime()))
                    .append(" - ")
                    .append(formatTime(settings.closingTime()))
                    .append("\n");
        }

        if (settings.wifiName() != null && !settings.wifiName().isBlank()) {
            sb.append("  - WiFi: ").append(settings.wifiName()).append("\n");
        }

        sb.append("  - Thanh toán tiền mặt: ")
                .append(Boolean.TRUE.equals(settings.cashPaymentEnabled()) ? "Có" : "Không")
                .append("\n");
        sb.append("  - Thanh toán online: ")
                .append(Boolean.TRUE.equals(settings.onlinePaymentEnabled()) ? "Có" : "Không")
                .append("\n");

        return sb.toString();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("  - ").append(label).append(": ").append(value).append("\n");
        }
    }

    private String formatTime(LocalTime time) {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }
}
