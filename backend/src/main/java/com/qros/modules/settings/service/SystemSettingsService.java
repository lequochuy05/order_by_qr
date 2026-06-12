package com.qros.modules.settings.service;

import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.settings.dto.request.SystemSettingsUpdateRequest;
import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.dto.response.SystemSettingsResponse;
import com.qros.modules.settings.mapper.SystemSettingsMapper;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.repository.SystemSettingsRepository;
import com.qros.shared.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final SystemSettingsRepository settingsRepository;
    private final SystemSettingsMapper settingsMapper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SETTINGS, key = "'admin'")
    public SystemSettingsResponse getSettings() {
        return settingsMapper.toResponse(getOrCreateSettings());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SETTINGS, key = "'public'")
    public PublicSettingsResponse getPublicSettings() {
        return settingsMapper.toPublicResponse(getOrCreateSettings());
    }

    @Transactional
    @CacheEvict(value = CacheNames.SETTINGS, allEntries = true)
    public SystemSettingsResponse updateSettings(@NonNull SystemSettingsUpdateRequest request) {
        SystemSettings settings = getOrCreateSettings();

        settingsMapper.updateEntity(settings, request);

        SystemSettings saved = settingsRepository.save(settings);
        PublicSettingsResponse publicSettings = settingsMapper.toPublicResponse(saved);

        notificationService.notifySettingsChange(publicSettings);

        return settingsMapper.toResponse(saved);
    }

    @Transactional
    public SystemSettings getOrCreateSettings() {
        return settingsRepository.findById(SETTINGS_ID)
                .orElseGet(() -> settingsRepository.save(settingsMapper.defaultSettings()));
    }
}
