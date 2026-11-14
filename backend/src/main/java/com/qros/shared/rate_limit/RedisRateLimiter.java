package com.qros.shared.rate_limit;

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

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                return redis.call('PTTL', KEYS[1])
            end
            return -1
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;

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
                        "Rate-limit store unavailable for scope={}; allowing request without increment: {}",
                        scope,
                        exception.getMessage());
                return;
            }
            throw new BusinessException(
                    ErrorCode.FEATURE_DISABLED,
                    "This feature is temporarily unavailable because quota checks cannot be completed.");
        }

        if (retryAfterMs != null && retryAfterMs >= 0) {
            long retryAfterSeconds = Math.max(1, (retryAfterMs + 999) / 1000);
            throw new BusinessException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Too many requests. Please try again later.",
                    Map.of("retryAfterSeconds", retryAfterSeconds));
        }
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9:_-]", "_");
    }

    private String digest(String value) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
