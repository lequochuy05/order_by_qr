package com.qros.modules.settings.service;

import com.qros.modules.settings.dto.request.SystemSettingsUpdateRequest;
import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.dto.response.SystemSettingsResponse;
import com.qros.modules.settings.mapper.SystemSettingsMapper;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.repository.SystemSettingsRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final SystemSettingsRepository settingsRepository;
    private final SystemSettingsMapper settingsMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionSideEffectService sideEffects;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SETTINGS, key = "'admin'")
    public SystemSettingsResponse getSettings() {
        return settingsMapper.toResponse(loadSettingsEntity());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SETTINGS, key = "'public'")
    public PublicSettingsResponse getPublicSettings() {
        return settingsMapper.toPublicResponse(loadSettingsEntity());
    }

    @Transactional
    public SystemSettingsResponse updateSettings(
            @NonNull SystemSettingsUpdateRequest request, @NonNull String actorEmail) {
        SystemSettings settings = loadSettingsEntity();
        validateRequestVersion(settings, request.version());
        List<String> changedFields = detectChangedFields(settings, request);

        settingsMapper.updateEntity(settings, request);

        SystemSettings saved = settingsRepository.save(settings);
        PublicSettingsResponse publicSettings = settingsMapper.toPublicResponse(saved);

        // Cache invalidation and notification listeners both consume this event after commit.
        eventPublisher.publishEvent(new SettingsChangeEvent(publicSettings));
        sideEffects.afterCommit(
                () -> log.info(
                        "System settings updated by {}, changedFields={}",
                        actorEmail,
                        changedFields.isEmpty() ? List.of("none") : changedFields),
                "write system settings audit log");

        return settingsMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SystemSettings getSettingsEntity() {
        return loadSettingsEntity();
    }

    private SystemSettings loadSettingsEntity() {
        return settingsRepository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTINGS_NOT_FOUND));
    }

    private void validateRequestVersion(SystemSettings settings, Long requestVersion) {
        if (requestVersion != null && !Objects.equals(settings.getVersion(), requestVersion)) {
            throw new BusinessException(
                    ErrorCode.CONCURRENT_MODIFICATION,
                    "System settings were updated by another request. Reload and try again.");
        }
    }

    private List<String> detectChangedFields(SystemSettings settings, SystemSettingsUpdateRequest request) {
        List<String> changedFields = new ArrayList<>();
        addIfChanged(
                changedFields, "restaurantName", settings.getRestaurantName(), normalize(request.restaurantName()));
        addIfChanged(
                changedFields,
                "restaurantPhone",
                settings.getRestaurantPhone(),
                normalizeNullable(request.restaurantPhone()));
        addIfChanged(
                changedFields,
                "restaurantEmail",
                settings.getRestaurantEmail(),
                normalizeNullable(request.restaurantEmail()));
        addIfChanged(
                changedFields,
                "restaurantAddress",
                settings.getRestaurantAddress(),
                normalizeNullable(request.restaurantAddress()));
        addIfChanged(changedFields, "logoUrl", settings.getLogoUrl(), normalizeNullable(request.logoUrl()));
        addIfChanged(changedFields, "wifiName", settings.getWifiName(), normalizeNullable(request.wifiName()));
        addIfChanged(
                changedFields, "wifiPassword", settings.getWifiPassword(), normalizeNullable(request.wifiPassword()));
        addIfChanged(changedFields, "openingTime", settings.getOpeningTime(), request.openingTime());
        addIfChanged(changedFields, "closingTime", settings.getClosingTime(), request.closingTime());
        addIfChanged(changedFields, "currency", settings.getCurrency(), normalizeCurrency(request.currency()));
        addIfChanged(changedFields, "taxPercent", settings.getTaxPercent(), request.taxPercent());
        addIfChanged(
                changedFields,
                "serviceChargePercent",
                settings.getServiceChargePercent(),
                request.serviceChargePercent());
        addIfChanged(changedFields, "orderingEnabled", settings.getOrderingEnabled(), request.orderingEnabled());
        addIfChanged(changedFields, "maintenanceMode", settings.getMaintenanceMode(), request.maintenanceMode());
        return List.copyOf(changedFields);
    }

    private void addIfChanged(List<String> changedFields, String field, Object currentValue, Object newValue) {
        if (!valuesEqual(currentValue, newValue)) {
            changedFields.add(field);
        }
    }

    private boolean valuesEqual(Object currentValue, Object newValue) {
        if (currentValue instanceof BigDecimal currentDecimal && newValue instanceof BigDecimal newDecimal) {
            return currentDecimal.compareTo(newDecimal) == 0;
        }
        return Objects.equals(currentValue, newValue);
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeCurrency(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
