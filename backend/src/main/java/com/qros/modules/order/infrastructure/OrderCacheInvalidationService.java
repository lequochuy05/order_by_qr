package com.qros.modules.order.infrastructure;

import com.qros.shared.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * OrderCacheInvalidationService - Dedicated service for clearing caches related
 * to Order mutations.
 * Centralizes cache eviction logic to avoid scattered @CacheEvict annotations
 * and to gracefully handle manual eviction needs for complex workflows.
 */
@Service
@RequiredArgsConstructor
public class OrderCacheInvalidationService {

    private final CacheManager cacheManager;

    /**
     * Evicts all caches affected by an order mutation (creation, update, status
     * change, deletion).
     *
     * @param orderId The ID of the mutated order
     */
    public void evictAfterOrderMutation(com.qros.modules.order.model.Order order) {
        if (order == null) return;
        evict(CacheNames.ORDER_BY_ID, order.getId());

        if (order.getTable() != null) {
            String tableCode = order.getTable().getTableCode();
            evict(CacheNames.TABLES, "code_" + tableCode);
            evict(CacheNames.TABLES, "public_code_" + tableCode);
        }
        evict(CacheNames.TABLES, "all_sorted");

        clear(CacheNames.ORDER_STATS);
        clear(CacheNames.ANALYTICS);
        clear(CacheNames.STATS_DASHBOARD);
    }

    private void evict(String cacheName, Object key) {
        if (key == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
