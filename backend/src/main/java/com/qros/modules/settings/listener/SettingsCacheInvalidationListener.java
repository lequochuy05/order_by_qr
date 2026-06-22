package com.qros.modules.settings.listener;

import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.SettingsChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SettingsCacheInvalidationListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSettingsChange(SettingsChangeEvent event) {
        Cache cache = cacheManager.getCache(CacheNames.SETTINGS);
        if (cache != null) {
            cache.clear();
        }
    }
}
