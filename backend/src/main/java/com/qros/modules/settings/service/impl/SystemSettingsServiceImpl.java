package com.qros.modules.settings.service.impl;

import com.qros.modules.settings.dto.SystemSettingsDto;
import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.repository.SystemSettingsRepository;
import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.settings.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemSettingsServiceImpl implements SystemSettingsService {
    private final SystemSettingsRepository repository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @Cacheable(value = "settings", key = "#includeSensitive ? 'current_admin' : 'current_public'")
    public SystemSettingsDto getCurrent(boolean includeSensitive) {
        return toDto(getOrCreateSettings(), includeSensitive);
    }

    @Override
    @Transactional
    @Cacheable(value = "settings", key = "'customer_public'")
    public PublicMenuResponse.Settings getPublicSettings() {
        return PublicMenuResponse.fromSettings(getOrCreateSettings());
    }

    @Override
    @Transactional
    @CacheEvict(value = "settings", allEntries = true)
    public SystemSettingsDto update(@NonNull SystemSettingsDto request) {
        SystemSettings settings = getOrCreateSettings();
        settings.setRestaurantName(request.getRestaurantName().trim());
        settings.setRestaurantAddress(trimToNull(request.getRestaurantAddress()));
        settings.setRestaurantPhone(trimToNull(request.getRestaurantPhone()));
        settings.setRestaurantLogoUrl(trimToNull(request.getRestaurantLogoUrl()));
        settings.setVatRate(request.getVatRate() != null ? request.getVatRate() : BigDecimal.ZERO);
        settings.setWifiSsid(trimToNull(request.getWifiSsid()));
        settings.setWifiPassword(trimToNull(request.getWifiPassword()));
        settings.setAutoApproveOrders(Boolean.TRUE.equals(request.getAutoApproveOrders()));
        settings.setEnableAiAssistant(Boolean.TRUE.equals(request.getEnableAiAssistant()));
        settings.setEnablePayos(Boolean.TRUE.equals(request.getEnablePayos()));
        settings.setEnableCash(Boolean.TRUE.equals(request.getEnableCash()));

        SystemSettings saved = repository.save(settings);
        notificationService.notifySettingsChange(PublicMenuResponse.fromSettings(saved));
        return toDto(saved, true);
    }

    private SystemSettings getOrCreateSettings() {
        return repository.findTopByOrderByIdAsc()
                .orElseGet(() -> repository.save(SystemSettings.builder()
                        .restaurantName("Sắc Màu Quán")
                        .restaurantAddress("Vu Gia - Đà Nẵng")
                        .restaurantPhone("0704102569")
                        .vatRate(BigDecimal.valueOf(8.0))
                        .autoApproveOrders(true)
                        .enableAiAssistant(true)
                        .enablePayos(true)
                        .enableCash(true)
                        .build()));
    }

    private SystemSettingsDto toDto(SystemSettings settings, boolean includeSensitive) {
        return SystemSettingsDto.builder()
                .id(settings.getId())
                .restaurantName(settings.getRestaurantName())
                .restaurantAddress(settings.getRestaurantAddress())
                .restaurantPhone(settings.getRestaurantPhone())
                .restaurantLogoUrl(settings.getRestaurantLogoUrl())
                .vatRate(settings.getVatRate())
                .wifiSsid(settings.getWifiSsid())
                .wifiPassword(includeSensitive ? settings.getWifiPassword() : null)
                .autoApproveOrders(settings.getAutoApproveOrders())
                .enableAiAssistant(settings.getEnableAiAssistant())
                .enablePayos(settings.getEnablePayos())
                .enableCash(settings.getEnableCash())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
