package com.qros.modules.auth.store;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String STORED_VALUE = "1";
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GETDEL', KEYS[1]); " + "if value then return 1 else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void create(@NonNull String key, long ttlMs) {
        if (ttlMs <= 0) {
            throw new IllegalArgumentException("Refresh token TTL must be positive");
        }

        try {
            redisTemplate.opsForValue().set(key, STORED_VALUE, Duration.ofMillis(ttlMs));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Refresh token store is unavailable", e);
        }
    }

    @Override
    public boolean consumeAtomically(@NonNull String key) {
        try {
            Long consumed = redisTemplate.execute(CONSUME_SCRIPT, List.of(key));
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
