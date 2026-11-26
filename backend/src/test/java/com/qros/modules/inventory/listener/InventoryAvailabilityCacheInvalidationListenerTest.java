package com.qros.modules.inventory.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.InventoryChangeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class InventoryAvailabilityCacheInvalidationListenerTest {

    @Test
    void createdInventoryItemOnlyClearsInventoryCacheWithoutThrashingPublicMenu() {
        CacheManager cacheManager = mock(CacheManager.class);
        NotificationService notificationService = mock(NotificationService.class);
        Cache inventoryCache = mock(Cache.class);
        Cache publicMenuCache = mock(Cache.class);
        when(cacheManager.getCache(CacheNames.INVENTORY)).thenReturn(inventoryCache);
        when(cacheManager.getCache(CacheNames.PUBLIC_MENU)).thenReturn(publicMenuCache);

        var listener = new InventoryAvailabilityCacheInvalidationListener(cacheManager, notificationService);
        listener.onInventoryChange(new InventoryChangeEvent("inventory_item_created", 10L));

        verify(inventoryCache).clear();
        verify(publicMenuCache, never()).clear();
        verify(notificationService, never()).notifyMenuAvailabilityChange();
    }

    @Test
    void stockInClearsPublicCatalogCachesAndNotifiesAvailabilityChange() {
        CacheManager cacheManager = mock(CacheManager.class);
        NotificationService notificationService = mock(NotificationService.class);
        Cache inventoryCache = mock(Cache.class);
        Cache publicMenuCache = mock(Cache.class);
        when(cacheManager.getCache(CacheNames.INVENTORY)).thenReturn(inventoryCache);
        when(cacheManager.getCache(CacheNames.PUBLIC_MENU)).thenReturn(publicMenuCache);

        var listener = new InventoryAvailabilityCacheInvalidationListener(cacheManager, notificationService);
        listener.onInventoryChange(new InventoryChangeEvent("stock_in", 10L));

        verify(inventoryCache).clear();
        verify(publicMenuCache).clear();
        verify(notificationService).notifyMenuAvailabilityChange();
    }
}
