package com.qros.modules.auth.store;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRefreshTokenStore.class);

    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value == ARGV[1] then redis.call('DEL', KEYS[1]); return 1 else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void create(@NonNull String key, @NonNull String value, long ttlMs) {
        if (ttlMs <= 0) {
            throw new IllegalArgumentException("Refresh token TTL must be positive");
        }

        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofMillis(ttlMs));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Refresh token store is unavailable", e);
        }
    }

    @Override
    public String get(@NonNull String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Refresh token store is unavailable", e);
        }
    }

    @Override
    public boolean consumeAtomically(@NonNull String key, @NonNull String expectedValue) {
        try {
            Long consumed = redisTemplate.execute(CONSUME_SCRIPT, List.of(key), expectedValue);
            return Long.valueOf(1L).equals(consumed);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Refresh token store is unavailable", e);
        }
    }

    @Override
    public void revoke(@NonNull String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("Unable to revoke refresh token because the token store is unavailable");
        }
    }
}
