package com.qros.modules.inventory.listener;

import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.InventoryChangeEvent;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InventoryAvailabilityCacheInvalidationListener {

    /**
     * Creating a brand-new inventory item (not attached to any recipe yet) or deleting
     * an unused inventory item cannot change public menu or combo availability.
     */
    private static final Set<String> INVENTORY_ONLY_EVENTS = Set.of("inventory_item_created", "inventory_item_deleted");

    private final CacheManager cacheManager;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInventoryChange(InventoryChangeEvent event) {
        clear(CacheNames.INVENTORY);
        if (event != null && event.type() != null && INVENTORY_ONLY_EVENTS.contains(event.type())) {
            return;
        }
        clear(CacheNames.MENU);
        clear(CacheNames.COMBOS);
        clear(CacheNames.MENU_AVAILABILITY);
        clear(CacheNames.PUBLIC_MENU);
        clear(CacheNames.RECOMMENDATIONS);
        clear(CacheNames.AI_MENU_CONTEXT);
        notificationService.notifyMenuAvailabilityChange();
    }

    private void clear(String cacheName) {
        com.qros.shared.cache.CacheHelper.clear(cacheManager, cacheName);
    }
}
