package com.qros.shared.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * TokenBlacklistService - Provides fast Redis-backed token blacklisting for revoked access tokens,
 * backed by a bounded local Caffeine fallback cache during transient Redis outages.
 * Tokens are stored as SHA-256 hashes with TTL equal to their remaining validity.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "auth:blacklist:token:";
    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;
    private final Cache<String, Long> localFallbackBlacklist = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofHours(2))
            .build();

    public TokenBlacklistService(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public void blacklistToken(@NonNull String token) {
        if (!jwtService.isValid(token)) {
            return;
        }
        Date expiration = jwtService.extractExpiration(token);
        if (expiration == null) {
            return;
        }
        long remainingMs = expiration.getTime() - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return;
        }
        String hash = hashToken(token);
        localFallbackBlacklist.put(hash, expiration.getTime());
        try {
            redisTemplate
                    .opsForValue()
                    .set(BLACKLIST_PREFIX + hash, "revoked", Objects.requireNonNull(Duration.ofMillis(remainingMs)));
            log.info("Blacklisted token hash={} for remaining duration {}ms", hash, remainingMs);
        } catch (RuntimeException ex) {
            log.warn("Failed to blacklist token in Redis; retained in local fallback cache: {}", ex.getMessage());
        }
    }

    public boolean isBlacklisted(@NonNull String token) {
        String hash = hashToken(token);
        Long localExpiry = localFallbackBlacklist.getIfPresent(hash);
        if (localExpiry != null) {
            if (localExpiry > System.currentTimeMillis()) {
                return true;
            }
            localFallbackBlacklist.invalidate(hash);
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + hash));
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable during token blacklist check: {}", ex.getMessage());
            return false;
        }
    }

    private String hashToken(String token) {
        return HashUtils.sha256Hex(token);
    }
}
