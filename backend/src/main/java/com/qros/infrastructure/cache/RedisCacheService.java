package com.qros.infrastructure.cache;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * RedisCache - Implementation of CacheService using RedisTemplate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService implements CacheService {

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
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Cache get: key={}, found={}", key, value != null);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
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
