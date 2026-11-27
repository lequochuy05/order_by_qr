package com.qros.shared.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public final class CacheHelper {

    private CacheHelper() {}

    public static void evict(CacheManager cacheManager, String cacheName, Object key) {
        if (cacheManager == null || key == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(key);
        }
    }

    public static void clear(CacheManager cacheManager, String... cacheNames) {
        if (cacheManager == null || cacheNames == null) {
            return;
        }
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
