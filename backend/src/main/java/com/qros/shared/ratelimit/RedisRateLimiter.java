package com.qros.shared.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiter {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                return redis.call('PTTL', KEYS[1])
            end
            return -1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    private final Cache<String, LocalWindowBucket> localFallbackCache = Caffeine.newBuilder()
            .maximumSize(20_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public void requireAllowed(
            @NonNull String scope, @NonNull String subject, int maxRequests, @NonNull Duration window) {
        requireAllowed(scope, subject, maxRequests, window, true);
    }

    public void requireAllowed(
            @NonNull String scope,
            @NonNull String subject,
            int maxRequests,
            @NonNull Duration window,
            boolean failOpen) {
        if (maxRequests <= 0 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate-limit policy must use positive values");
        }

        String key = "rate-limit:" + sanitize(scope) + ":" + digest(subject);
        Long retryAfterMs;
        try {
            retryAfterMs = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT, List.of(key), Long.toString(window.toMillis()), Integer.toString(maxRequests));
        } catch (RuntimeException exception) {
            if (failOpen) {
                log.warn(
                        "Rate-limit store unavailable for scope={}; enforcing bounded local fallback: {}",
                        scope,
                        exception.getMessage());
                retryAfterMs = checkLocalFallback(key, maxRequests, window);
            } else {
                throw new BusinessException(
                        ErrorCode.FEATURE_DISABLED,
                        "This feature is temporarily unavailable because quota checks cannot be completed.");
            }
        }

        if (retryAfterMs != null && retryAfterMs >= 0) {
            long retryAfterSeconds = Math.max(1, (retryAfterMs + 999) / 1000);
            throw new BusinessException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Too many requests. Please try again later.",
                    Map.of("retryAfterSeconds", retryAfterSeconds));
        }
    }

    private Long checkLocalFallback(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = window.toMillis();
        LocalWindowBucket bucket = localFallbackCache.get(key, k -> new LocalWindowBucket(now + windowMs));
        if (bucket == null) {
            return -1L;
        }
        synchronized (bucket) {
            if (now >= bucket.windowEndMillis) {
                bucket.windowEndMillis = now + windowMs;
                bucket.count = 1;
                return -1L;
            }
            bucket.count++;
            if (bucket.count > maxRequests) {
                return Math.max(1L, bucket.windowEndMillis - now);
            }
            return -1L;
        }
    }

    private static final class LocalWindowBucket {
        private long windowEndMillis;
        private int count;

        private LocalWindowBucket(long windowEndMillis) {
            this.windowEndMillis = windowEndMillis;
            this.count = 0;
        }
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9:_-]", "_");
    }

    private String digest(String value) {
        return com.qros.shared.security.HashUtils.sha256Hex(value);
    }
}
