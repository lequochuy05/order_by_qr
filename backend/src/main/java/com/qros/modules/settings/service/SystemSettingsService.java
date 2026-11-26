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
import com.qros.shared.transaction.TransactionExecutor;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.validation.ImageFileValidator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
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
    private final TransactionExecutor transactionExecutor;
    private final SystemSettingsSnapshotCache snapshotCache;

    public SystemSettingsService(
            SystemSettingsRepository settingsRepository,
            SystemSettingsMapper settingsMapper,
            ApplicationEventPublisher eventPublisher,
            TransactionSideEffectService sideEffects,
            StorageService storageService,
            ImageFileValidator imageFileValidator,
            TransactionExecutor transactionExecutor) {
        this(
                settingsRepository,
                settingsMapper,
                eventPublisher,
                sideEffects,
                storageService,
                imageFileValidator,
                transactionExecutor,
                null);
    }

    @Autowired
    public SystemSettingsService(
            SystemSettingsRepository settingsRepository,
            SystemSettingsMapper settingsMapper,
            ApplicationEventPublisher eventPublisher,
            TransactionSideEffectService sideEffects,
            StorageService storageService,
            ImageFileValidator imageFileValidator,
            TransactionExecutor transactionExecutor,
            SystemSettingsSnapshotCache snapshotCache) {
        this.settingsRepository = settingsRepository;
        this.settingsMapper = settingsMapper;
        this.eventPublisher = eventPublisher;
        this.sideEffects = sideEffects;
        this.storageService = storageService;
        this.imageFileValidator = imageFileValidator;
        this.transactionExecutor = transactionExecutor;
        this.snapshotCache = snapshotCache;
    }

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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SystemSettingsResponse uploadLogo(@NonNull MultipartFile file, @NonNull String actorEmail) {
        imageFileValidator.validate(file);
        transactionExecutor.readOnly(() -> {
            loadSettingsEntity();
            return null;
        });

        String newUrl = uploadLogoOutsideTransaction(file);

        try {
            return transactionExecutor.write(() -> {
                SystemSettings settings = loadSettingsEntity();
                String oldUrl = settings.getLogoUrl();

                settings.setLogoUrl(newUrl);
                SystemSettings saved = settingsRepository.saveAndFlush(settings);

                if (oldUrl != null && !oldUrl.isBlank() && oldUrl.startsWith("http")) {
                    sideEffects.afterCommit(() -> storageService.delete(oldUrl), "delete replaced restaurant logo");
                }

                eventPublisher.publishEvent(new SettingsChangeEvent(settingsMapper.toPublicResponse(saved)));
                sideEffects.afterCommit(
                        () -> log.info("Restaurant logo updated by {}", actorEmail), "write restaurant logo audit log");
                return settingsMapper.toResponse(saved);
            });
        } catch (RuntimeException exception) {
            deleteUploadedLogoSilently(newUrl);
            throw exception;
        }
    }

    private String uploadLogoOutsideTransaction(MultipartFile file) {
        try {
            return storageService.upload(file, LOGO_FOLDER);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Unable to upload restaurant logo: {}", exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload restaurant logo", exception);
        }
    }

    private void deleteUploadedLogoSilently(String uploadedUrl) {
        try {
            storageService.delete(uploadedUrl);
        } catch (Exception cleanupException) {
            log.warn("Failed to clean up rolled back restaurant logo upload: {}", cleanupException.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public SystemSettings getSettingsEntity() {
        if (snapshotCache == null) {
            return loadSettingsEntity();
        }
        return toDetachedEntity(snapshotCache.loadSnapshot());
    }

    private SystemSettings toDetachedEntity(SystemSettingsResponse snapshot) {
        return SystemSettings.builder()
                .id(snapshot.id())
                .version(snapshot.version())
                .restaurantName(snapshot.restaurantName())
                .restaurantPhone(snapshot.restaurantPhone())
                .restaurantEmail(snapshot.restaurantEmail())
                .restaurantAddress(snapshot.restaurantAddress())
                .logoUrl(snapshot.logoUrl())
                .wifiName(snapshot.wifiName())
                .wifiPassword(snapshot.wifiPassword())
                .openingTime(snapshot.openingTime())
                .closingTime(snapshot.closingTime())
                .currency(snapshot.currency())
                .taxPercent(snapshot.taxPercent())
                .serviceChargePercent(snapshot.serviceChargePercent())
                .orderingEnabled(snapshot.orderingEnabled())
                .maintenanceMode(snapshot.maintenanceMode())
                .cashPaymentEnabled(snapshot.cashPaymentEnabled())
                .onlinePaymentEnabled(snapshot.onlinePaymentEnabled())
                .paymentQrExpiresInMinutes(snapshot.paymentQrExpiresInMinutes())
                .autoConfirmOrders(snapshot.autoConfirmOrders())
                .kitchenOverdueThresholdMinutes(snapshot.kitchenOverdueThresholdMinutes())
                .showUnavailableItems(snapshot.showUnavailableItems())
                .showRecommendations(snapshot.showRecommendations())
                .showCombos(snapshot.showCombos())
                .billTitle(snapshot.billTitle())
                .billFooterMessage(snapshot.billFooterMessage())
                .billPaperSize(snapshot.billPaperSize())
                .showWifiOnBill(snapshot.showWifiOnBill())
                .autoPrintBill(snapshot.autoPrintBill())
                .newOrderNotificationEnabled(snapshot.newOrderNotificationEnabled())
                .paymentNotificationEnabled(snapshot.paymentNotificationEnabled())
                .kitchenOverdueNotificationEnabled(snapshot.kitchenOverdueNotificationEnabled())
                .allowSettlementWithUnfinishedItems(snapshot.allowSettlementWithUnfinishedItems())
                .customerSelfCheckoutEnabled(snapshot.customerSelfCheckoutEnabled())
                .build();
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

    private static final List<SettingFieldDiff> SETTING_FIELD_DIFFS = List.of(
            diff("restaurantName", SystemSettings::getRestaurantName, r -> normalize(r.restaurantName())),
            diff("restaurantPhone", SystemSettings::getRestaurantPhone, r -> normalizeNullable(r.restaurantPhone())),
            diff("restaurantEmail", SystemSettings::getRestaurantEmail, r -> normalizeNullable(r.restaurantEmail())),
            diff(
                    "restaurantAddress",
                    SystemSettings::getRestaurantAddress,
                    r -> normalizeNullable(r.restaurantAddress())),
            diff("logoUrl", SystemSettings::getLogoUrl, r -> normalizeNullable(r.logoUrl())),
            diff("wifiName", SystemSettings::getWifiName, r -> normalizeNullable(r.wifiName())),
            diff("wifiPassword", SystemSettings::getWifiPassword, r -> normalizeNullable(r.wifiPassword())),
            diff("openingTime", SystemSettings::getOpeningTime, SystemSettingsUpdateRequest::openingTime),
            diff("closingTime", SystemSettings::getClosingTime, SystemSettingsUpdateRequest::closingTime),
            diff("currency", SystemSettings::getCurrency, r -> normalizeCurrency(r.currency())),
            diff("taxPercent", SystemSettings::getTaxPercent, SystemSettingsUpdateRequest::taxPercent),
            diff(
                    "serviceChargePercent",
                    SystemSettings::getServiceChargePercent,
                    SystemSettingsUpdateRequest::serviceChargePercent),
            diff("orderingEnabled", SystemSettings::getOrderingEnabled, SystemSettingsUpdateRequest::orderingEnabled),
            diff("maintenanceMode", SystemSettings::getMaintenanceMode, SystemSettingsUpdateRequest::maintenanceMode),
            diff(
                    "cashPaymentEnabled",
                    SystemSettings::getCashPaymentEnabled,
                    SystemSettingsUpdateRequest::cashPaymentEnabled),
            diff(
                    "onlinePaymentEnabled",
                    SystemSettings::getOnlinePaymentEnabled,
                    SystemSettingsUpdateRequest::onlinePaymentEnabled),
            diff(
                    "paymentQrExpiresInMinutes",
                    SystemSettings::getPaymentQrExpiresInMinutes,
                    SystemSettingsUpdateRequest::paymentQrExpiresInMinutes),
            diff(
                    "autoConfirmOrders",
                    SystemSettings::getAutoConfirmOrders,
                    SystemSettingsUpdateRequest::autoConfirmOrders),
            diff(
                    "kitchenOverdueThresholdMinutes",
                    SystemSettings::getKitchenOverdueThresholdMinutes,
                    SystemSettingsUpdateRequest::kitchenOverdueThresholdMinutes),
            diff(
                    "showUnavailableItems",
                    SystemSettings::getShowUnavailableItems,
                    SystemSettingsUpdateRequest::showUnavailableItems),
            diff(
                    "showRecommendations",
                    SystemSettings::getShowRecommendations,
                    SystemSettingsUpdateRequest::showRecommendations),
            diff("showCombos", SystemSettings::getShowCombos, SystemSettingsUpdateRequest::showCombos),
            diff("billTitle", SystemSettings::getBillTitle, r -> normalize(r.billTitle())),
            diff("billFooterMessage", SystemSettings::getBillFooterMessage, r -> normalize(r.billFooterMessage())),
            diff("billPaperSize", SystemSettings::getBillPaperSize, SystemSettingsUpdateRequest::billPaperSize),
            diff("showWifiOnBill", SystemSettings::getShowWifiOnBill, SystemSettingsUpdateRequest::showWifiOnBill),
            diff("autoPrintBill", SystemSettings::getAutoPrintBill, SystemSettingsUpdateRequest::autoPrintBill),
            diff(
                    "newOrderNotificationEnabled",
                    SystemSettings::getNewOrderNotificationEnabled,
                    SystemSettingsUpdateRequest::newOrderNotificationEnabled),
            diff(
                    "paymentNotificationEnabled",
                    SystemSettings::getPaymentNotificationEnabled,
                    SystemSettingsUpdateRequest::paymentNotificationEnabled),
            diff(
                    "kitchenOverdueNotificationEnabled",
                    SystemSettings::getKitchenOverdueNotificationEnabled,
                    SystemSettingsUpdateRequest::kitchenOverdueNotificationEnabled),
            diff(
                    "allowSettlementWithUnfinishedItems",
                    SystemSettings::getAllowSettlementWithUnfinishedItems,
                    SystemSettingsUpdateRequest::allowSettlementWithUnfinishedItems),
            diff(
                    "customerSelfCheckoutEnabled",
                    SystemSettings::getCustomerSelfCheckoutEnabled,
                    SystemSettingsUpdateRequest::customerSelfCheckoutEnabled));

    private List<String> detectChangedFields(SystemSettings settings, SystemSettingsUpdateRequest request) {
        return SETTING_FIELD_DIFFS.stream()
                .filter(entry -> !valuesEqual(
                        entry.currentExtractor().apply(settings),
                        entry.nextExtractor().apply(request)))
                .map(SettingFieldDiff::field)
                .toList();
    }

    private static SettingFieldDiff diff(
            String field,
            Function<SystemSettings, Object> currentExtractor,
            Function<SystemSettingsUpdateRequest, Object> nextExtractor) {
        return new SettingFieldDiff(field, currentExtractor, nextExtractor);
    }

    private static boolean valuesEqual(Object currentValue, Object newValue) {
        if (currentValue instanceof BigDecimal currentDecimal && newValue instanceof BigDecimal newDecimal) {
            return currentDecimal.compareTo(newDecimal) == 0;
        }
        return Objects.equals(currentValue, newValue);
    }

    private static String normalize(String value) {
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeCurrency(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private record SettingFieldDiff(
            String field,
            Function<SystemSettings, Object> currentExtractor,
            Function<SystemSettingsUpdateRequest, Object> nextExtractor) {}
}
