package com.qros.modules.menu.listener;

import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.CategoryChangeEvent;
import com.qros.shared.event.DomainEvents.ComboChangeEvent;
import com.qros.shared.event.DomainEvents.MenuChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MenuCacheInvalidationListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMenuChange(MenuChangeEvent event) {
        clearMenuCaches();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCategoryChange(CategoryChangeEvent event) {
        clearMenuCaches();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onComboChange(ComboChangeEvent event) {
        clearMenuCaches();
    }

    private void clearMenuCaches() {
        clear(CacheNames.MENU);
        clear(CacheNames.PUBLIC_MENU);
        clear(CacheNames.CATEGORIES);
        clear(CacheNames.COMBOS);
        clear(CacheNames.RECOMMENDATIONS);
        clear(CacheNames.POPULAR_ITEMS);
        clear(CacheNames.AI_MENU_CONTEXT);
        clear(CacheNames.MENU_AVAILABILITY);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
