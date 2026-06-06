package com.qros.infrastructure.cache.impl;

import com.qros.infrastructure.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * CacheServiceImpl - Implementation of CacheService using RedisTemplate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void set(@NonNull String key, @NonNull Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Cache set: key={}, timeout={} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Failed to set cache for key: {}", key, e);
        }
    }

    @Override
    public Object get(@NonNull String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Cache get: key={}, found={}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("Failed to get cache for key: {}", key, e);
            return null;
        }
    }

    @Override
    public void delete(@NonNull String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Cache delete: key={}", key);
        } catch (Exception e) {
            log.error("Failed to delete cache for key: {}", key, e);
        }
    }

    @Override
    public boolean hasKey(@NonNull String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check existence of key: {}", key, e);
            return false;
        }
    }
}
