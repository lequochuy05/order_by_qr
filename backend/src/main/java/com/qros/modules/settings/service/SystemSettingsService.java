package com.qros.modules.settings.service;

import com.qros.infrastructure.storage.StorageService;
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
import com.qros.shared.validation.ImageFileValidator;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsService {

    private static final Long SETTINGS_ID = 1L;
    private static final String LOGO_FOLDER = "order_by_qr/settings";

    private final SystemSettingsRepository settingsRepository;
    private final SystemSettingsMapper settingsMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionSideEffectService sideEffects;
    private final StorageService storageService;
    private final ImageFileValidator imageFileValidator;

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

        SystemSettings saved = settingsRepository.saveAndFlush(settings);
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

    @Transactional
    public SystemSettingsResponse uploadLogo(@NonNull MultipartFile file, @NonNull String actorEmail) {
        imageFileValidator.validate(file);
        SystemSettings settings = loadSettingsEntity();
        String oldUrl = settings.getLogoUrl();

        try {
            String newUrl = storageService.upload(file, LOGO_FOLDER);
            sideEffects.afterRollback(() -> storageService.delete(newUrl), "delete rolled back restaurant logo upload");

            settings.setLogoUrl(newUrl);
            SystemSettings saved = settingsRepository.saveAndFlush(settings);

            if (oldUrl != null && !oldUrl.isBlank() && oldUrl.startsWith("http")) {
                sideEffects.afterCommit(() -> storageService.delete(oldUrl), "delete replaced restaurant logo");
            }

            eventPublisher.publishEvent(new SettingsChangeEvent(settingsMapper.toPublicResponse(saved)));
            sideEffects.afterCommit(
                    () -> log.info("Restaurant logo updated by {}", actorEmail), "write restaurant logo audit log");
            return settingsMapper.toResponse(saved);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Unable to upload restaurant logo: {}", exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload restaurant logo", exception);
        }
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
        addIfChanged(
                changedFields, "cashPaymentEnabled", settings.getCashPaymentEnabled(), request.cashPaymentEnabled());
        addIfChanged(
                changedFields,
                "onlinePaymentEnabled",
                settings.getOnlinePaymentEnabled(),
                request.onlinePaymentEnabled());
        addIfChanged(
                changedFields,
                "paymentQrExpiresInMinutes",
                settings.getPaymentQrExpiresInMinutes(),
                request.paymentQrExpiresInMinutes());
        addIfChanged(changedFields, "autoConfirmOrders", settings.getAutoConfirmOrders(), request.autoConfirmOrders());
        addIfChanged(
                changedFields,
                "kitchenOverdueThresholdMinutes",
                settings.getKitchenOverdueThresholdMinutes(),
                request.kitchenOverdueThresholdMinutes());
        addIfChanged(
                changedFields,
                "showUnavailableItems",
                settings.getShowUnavailableItems(),
                request.showUnavailableItems());
        addIfChanged(
                changedFields, "showRecommendations", settings.getShowRecommendations(), request.showRecommendations());
        addIfChanged(changedFields, "showCombos", settings.getShowCombos(), request.showCombos());
        addIfChanged(changedFields, "billTitle", settings.getBillTitle(), normalize(request.billTitle()));
        addIfChanged(
                changedFields,
                "billFooterMessage",
                settings.getBillFooterMessage(),
                normalize(request.billFooterMessage()));
        addIfChanged(changedFields, "billPaperSize", settings.getBillPaperSize(), request.billPaperSize());
        addIfChanged(changedFields, "showWifiOnBill", settings.getShowWifiOnBill(), request.showWifiOnBill());
        addIfChanged(changedFields, "autoPrintBill", settings.getAutoPrintBill(), request.autoPrintBill());
        addIfChanged(
                changedFields,
                "newOrderNotificationEnabled",
                settings.getNewOrderNotificationEnabled(),
                request.newOrderNotificationEnabled());
        addIfChanged(
                changedFields,
                "paymentNotificationEnabled",
                settings.getPaymentNotificationEnabled(),
                request.paymentNotificationEnabled());
        addIfChanged(
                changedFields,
                "kitchenOverdueNotificationEnabled",
                settings.getKitchenOverdueNotificationEnabled(),
                request.kitchenOverdueNotificationEnabled());
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
