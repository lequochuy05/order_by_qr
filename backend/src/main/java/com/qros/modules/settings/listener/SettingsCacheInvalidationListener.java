package com.qros.modules.settings.listener;

import com.qros.shared.cache.CacheHelper;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.SettingsChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SettingsCacheInvalidationListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSettingsChange(SettingsChangeEvent event) {
        CacheHelper.clear(cacheManager, CacheNames.SETTINGS);
        // Public catalog visibility flags (showUnavailableItems, showCombos) come from settings.
        CacheHelper.clear(cacheManager, CacheNames.PUBLIC_MENU);
    }
}
