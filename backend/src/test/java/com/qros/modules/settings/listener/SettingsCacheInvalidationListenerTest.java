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
    void settingsEventClearsSettingsAndPublicMenuCaches() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache settingsCache = mock(Cache.class);
        Cache publicMenuCache = mock(Cache.class);
        when(cacheManager.getCache(CacheNames.SETTINGS)).thenReturn(settingsCache);
        when(cacheManager.getCache(CacheNames.PUBLIC_MENU)).thenReturn(publicMenuCache);

        new SettingsCacheInvalidationListener(cacheManager).onSettingsChange(new SettingsChangeEvent(null));

        verify(settingsCache).clear();
        verify(publicMenuCache).clear();
    }
}
