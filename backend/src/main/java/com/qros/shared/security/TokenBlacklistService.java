package com.qros.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * TokenBlacklistService - Provides fast Redis-backed token blacklisting for revoked access tokens.
 * Used on explicit user logout so tokens cannot be reused within their 15-minute expiration window.
 * Tokens are stored as SHA-256 hashes with TTL equal to their remaining validity.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "auth:blacklist:token:";
    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public TokenBlacklistService(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public void blacklistToken(@NonNull String token) {
        try {
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
            redisTemplate
                    .opsForValue()
                    .set(BLACKLIST_PREFIX + hash, "revoked", Objects.requireNonNull(Duration.ofMillis(remainingMs)));
            log.info("Blacklisted token hash={} for remaining duration {}ms", hash, remainingMs);
        } catch (RuntimeException ex) {
            log.warn("Failed to blacklist token in Redis: {}", ex.getMessage());
        }
    }

    public boolean isBlacklisted(@NonNull String token) {
        try {
            String hash = hashToken(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + hash));
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable during token blacklist check: {}", ex.getMessage());
            return false;
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
