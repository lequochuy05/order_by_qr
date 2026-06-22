package com.qros.modules.settings.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.SettingsChangeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class SettingsCacheInvalidationListenerTest {

    @Test
    void settingsEventClearsSettingsCache() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(CacheNames.SETTINGS)).thenReturn(cache);

        new SettingsCacheInvalidationListener(cacheManager).onSettingsChange(new SettingsChangeEvent(null));

        verify(cache).clear();
    }
}
