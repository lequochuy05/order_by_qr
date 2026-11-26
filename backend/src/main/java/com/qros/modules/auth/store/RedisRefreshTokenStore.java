package com.qros.modules.auth.store;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRefreshTokenStore.class);
    private static final int MAX_SESSIONS_PER_USER = 10;

    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value == ARGV[1] then redis.call('DEL', KEYS[1]); return 1 else return 0 end",
            Long.class);

    private static final DefaultRedisScript<Long> CONSUME_SESSION_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value == ARGV[1] then redis.call('DEL', KEYS[1]); redis.call('SREM', KEYS[2], KEYS[1]); return 1 else return 0 end",
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

    @Override
    public void createSession(
            @NonNull String userIndexKey, @NonNull String sessionKey, @NonNull String value, long ttlMs) {
        if (ttlMs <= 0) {
            throw new IllegalArgumentException("Refresh token TTL must be positive");
        }

        try {
            Duration ttl = Duration.ofMillis(ttlMs);
            redisTemplate.opsForValue().set(sessionKey, value, ttl);
            redisTemplate.opsForSet().add(userIndexKey, sessionKey);
            redisTemplate.expire(userIndexKey, ttl);

            Long size = redisTemplate.opsForSet().size(userIndexKey);
            if (size != null && size > MAX_SESSIONS_PER_USER) {
                pruneExpiredOrExcessSessions(userIndexKey, sessionKey);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Refresh token store is unavailable", e);
        }
    }

    @Override
    public boolean consumeSessionAtomically(
            @NonNull String userIndexKey, @NonNull String sessionKey, @NonNull String expectedValue) {
        try {
            Long consumed =
                    redisTemplate.execute(CONSUME_SESSION_SCRIPT, List.of(sessionKey, userIndexKey), expectedValue);
            return Long.valueOf(1L).equals(consumed);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Refresh token store is unavailable", e);
        }
    }

    @Override
    public void revokeSession(@NonNull String userIndexKey, @NonNull String sessionKey) {
        try {
            redisTemplate.delete(sessionKey);
            redisTemplate.opsForSet().remove(userIndexKey, sessionKey);
        } catch (RuntimeException e) {
            log.warn("Unable to revoke refresh session because the token store is unavailable");
        }
    }

    @Override
    public void revokeAllUserSessions(@NonNull String userIndexKey, @NonNull String legacyKey) {
        try {
            Set<String> members = redisTemplate.opsForSet().members(userIndexKey);
            List<String> keysToDelete = new ArrayList<>();
            if (members != null && !members.isEmpty()) {
                keysToDelete.addAll(members);
            }
            keysToDelete.add(userIndexKey);
            keysToDelete.add(legacyKey);
            redisTemplate.delete(keysToDelete);
        } catch (RuntimeException e) {
            log.warn("Unable to revoke all user refresh sessions because the token store is unavailable");
        }
    }

    private void pruneExpiredOrExcessSessions(String userIndexKey, String keepKey) {
        Set<String> members = redisTemplate.opsForSet().members(userIndexKey);
        if (members == null || members.size() <= MAX_SESSIONS_PER_USER) {
            return;
        }
        List<String> activeOthers = new ArrayList<>();
        for (String member : members) {
            if (keepKey.equals(member)) {
                continue;
            }
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(member))) {
                redisTemplate.opsForSet().remove(userIndexKey, member);
            } else {
                activeOthers.add(member);
            }
        }
        int excess = activeOthers.size() + 1 - MAX_SESSIONS_PER_USER;
        for (int i = 0; i < excess && i < activeOthers.size(); i++) {
            String evicted = activeOthers.get(i);
            redisTemplate.delete(evicted);
            redisTemplate.opsForSet().remove(userIndexKey, evicted);
        }
    }
}
