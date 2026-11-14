package com.qros.modules.inventory.listener;

import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.InventoryChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InventoryAvailabilityCacheInvalidationListener {

    private final CacheManager cacheManager;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInventoryChange(InventoryChangeEvent event) {
        clear(CacheNames.INVENTORY);
        clear(CacheNames.MENU_AVAILABILITY);
        clear(CacheNames.PUBLIC_MENU);
        clear(CacheNames.RECOMMENDATIONS);
        clear(CacheNames.AI_MENU_CONTEXT);
        notificationService.notifyMenuAvailabilityChange();
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
