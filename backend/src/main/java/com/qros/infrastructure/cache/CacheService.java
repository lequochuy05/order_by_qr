package com.qros.infrastructure.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.lang.NonNull;

/**
 * CacheService - Generic interface for Redis caching operations.
 * Provides abstraction for get, set, and delete operations.
 */
public interface CacheService {

    /**
     * Stores a value in cache with a specific key and expiration.
     * 
     * @param key     Key to store
     * @param value   Value to store
     * @param timeout Time to live
     * @param unit    Time unit for timeout
     */
    void set(@NonNull String key, @NonNull Object value, long timeout, TimeUnit unit);

    /**
     * Retrieves a value from cache by key.
     * 
     * @param key Key to lookup
     * @return Cached object or null if not found
     */
    <T> T get(String key, Class<T> type);

    /**
     * Removes a value from cache by key.
     * 
     * @param key Key to delete
     */
    void delete(@NonNull String key);

    /**
     * Checks if a key exists in cache.
     * 
     * @param key Key to check
     * @return true if exists, false otherwise
     */
    boolean hasKey(@NonNull String key);
}
