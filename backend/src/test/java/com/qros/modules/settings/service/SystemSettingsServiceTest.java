package com.qros.modules.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.settings.dto.request.SystemSettingsUpdateRequest;
import com.qros.modules.settings.mapper.SystemSettingsMapper;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.repository.SystemSettingsRepository;
import com.qros.shared.event.DomainEvents.SettingsChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.validation.ImageFileValidator;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SystemSettingsServiceTest {

    private SystemSettingsRepository settingsRepository;
    private ApplicationEventPublisher eventPublisher;
    private TransactionSideEffectService sideEffects;
    private SystemSettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(SystemSettingsRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        sideEffects = mock(TransactionSideEffectService.class);
        settingsService = new SystemSettingsService(
                settingsRepository,
                new SystemSettingsMapper(),
                eventPublisher,
                sideEffects,
                mock(StorageService.class),
                new ImageFileValidator());
    }

    @Test
    void missingSingletonIsReportedWithoutLazyInsert() {
        when(settingsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(settingsService::getSettingsEntity)
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.SETTINGS_NOT_FOUND));

        verify(settingsRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatePublishesCommitAwareEventAndRegistersSafeAudit() {
        SystemSettings settings = settings();
        SystemSettingsUpdateRequest request = request("Updated Restaurant", "usd");
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        when(settingsRepository.saveAndFlush(settings)).thenAnswer(invocation -> {
            settings.setVersion(1L);
            return settings;
        });

        var response = settingsService.updateSettings(request, "manager@example.com");

        assertThat(response.restaurantName()).isEqualTo("Updated Restaurant");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.version()).isEqualTo(1L);
        verify(settingsRepository).saveAndFlush(settings);
        verify(eventPublisher).publishEvent(any(SettingsChangeEvent.class));
        verify(sideEffects).afterCommit(any(Runnable.class), eq("write system settings audit log"));
    }

    @Test
    void staleRequestVersionIsRejected() {
        SystemSettings settings = settings();
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        SystemSettingsUpdateRequest staleRequest = request("Updated Restaurant", "VND", 9L);

        assertThatThrownBy(() -> settingsService.updateSettings(staleRequest, "manager@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION));

        verify(settingsRepository, never()).saveAndFlush(any());
    }

    private SystemSettings settings() {
        return SystemSettings.builder()
                .id(1L)
                .version(0L)
                .restaurantName("QROS Restaurant")
                .currency("VND")
                .taxPercent(BigDecimal.ZERO)
                .serviceChargePercent(BigDecimal.ZERO)
                .orderingEnabled(true)
                .maintenanceMode(false)
                .cashPaymentEnabled(true)
                .onlinePaymentEnabled(true)
                .paymentQrExpiresInMinutes(20)
                .autoConfirmOrders(false)
                .kitchenOverdueThresholdMinutes(20)
                .showUnavailableItems(false)
                .showRecommendations(true)
                .showCombos(true)
                .billTitle("HÓA ĐƠN THANH TOÁN")
                .billFooterMessage("CẢM ƠN QUÝ KHÁCH")
                .billPaperSize("80")
                .showWifiOnBill(true)
                .autoPrintBill(true)
                .newOrderNotificationEnabled(true)
                .paymentNotificationEnabled(true)
                .kitchenOverdueNotificationEnabled(true)
                .build();
    }

    private SystemSettingsUpdateRequest request(String restaurantName, String currency) {
        return request(restaurantName, currency, 0L);
    }

    private SystemSettingsUpdateRequest request(String restaurantName, String currency, Long version) {
        return new SystemSettingsUpdateRequest(
                restaurantName,
                "0901234567",
                "contact@example.com",
                "123 Main Street",
                "https://example.com/logo.png",
                "QROS WiFi",
                "secret",
                LocalTime.of(18, 0),
                LocalTime.of(2, 0),
                currency,
                BigDecimal.TEN,
                BigDecimal.valueOf(5),
                true,
                false,
                true,
                true,
                20,
                false,
                20,
                false,
                true,
                true,
                "HÓA ĐƠN THANH TOÁN",
                "CẢM ƠN QUÝ KHÁCH",
                "80",
                true,
                true,
                true,
                true,
                true,
                version);
    }
}
