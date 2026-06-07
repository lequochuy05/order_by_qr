package com.qros.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * OrderCacheInvalidationService - Dedicated service for clearing caches related to Order mutations.
 * Centralizes cache eviction logic to avoid scattered @CacheEvict annotations
 * and to gracefully handle manual eviction needs for complex workflows.
 */
@Service
@RequiredArgsConstructor
public class OrderCacheInvalidationService {

    private final CacheManager cacheManager;

    /**
     * Evicts all caches affected by an order mutation (creation, update, status change, deletion).
     * 
     * @param orderId The ID of the mutated order
     */
    public void evictAfterOrderMutation(Long orderId) {
        evict("order_by_id", orderId);
        clear("tables");
        clear("order_stats");
        // Also clear any other caching layers related to orders in the future (e.g., revenue_stats)
    }

    private void evict(String cacheName, Object key) {
        if (key == null) return;
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
