package com.qros.modules.settings.service;

import com.qros.modules.settings.dto.response.SystemSettingsResponse;
import com.qros.modules.settings.mapper.SystemSettingsMapper;
import com.qros.modules.settings.repository.SystemSettingsRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caches an immutable {@link SystemSettingsResponse} snapshot instead of a mutable JPA entity
 * so Caffeine and Redis never hold or serialize live Hibernate entity state.
 */
@Component
@RequiredArgsConstructor
public class SystemSettingsSnapshotCache {

    private static final Long SETTINGS_ID = 1L;

    private final SystemSettingsRepository settingsRepository;
    private final SystemSettingsMapper settingsMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SETTINGS, key = "'snapshot'")
    public SystemSettingsResponse loadSnapshot() {
        return settingsMapper.toResponse(settingsRepository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTINGS_NOT_FOUND)));
    }
}
