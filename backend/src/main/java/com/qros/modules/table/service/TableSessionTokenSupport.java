package com.qros.modules.table.service;

import com.qros.modules.table.config.TableSessionProperties;
import com.qros.modules.table.model.TableSessionToken;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.HashUtils;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Pure helper encapsulating table-session client token generation, hashing, lifetime validation,
 * heartbeat throttling, and active slot eviction rules.
 */
final class TableSessionTokenSupport {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private TableSessionTokenSupport() {}

    static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID);
        }
        return HashUtils.sha256Hex(token.trim());
    }

    static void validateTokenLifetime(TableSessionToken token, TableSessionProperties properties, LocalDateTime now) {
        if (token.getIssuedAt() == null) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID);
        }
        if (isTokenExpired(token, properties, now)) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_EXPIRED);
        }
    }

    static boolean isTokenExpired(TableSessionToken token, TableSessionProperties properties, LocalDateTime now) {
        LocalDateTime issuedAt = token.getIssuedAt();
        if (issuedAt == null) {
            return true;
        }
        LocalDateTime lastSeenAt = token.getLastSeenAt() != null ? token.getLastSeenAt() : issuedAt;
        return issuedAt.isBefore(now.minus(properties.getTokenMaxLifetime()))
                || lastSeenAt.isBefore(now.minus(properties.getTokenIdleTimeout()));
    }

    static boolean shouldWriteHeartbeat(TableSessionToken token, TableSessionProperties properties, LocalDateTime now) {
        LocalDateTime lastSeenAt = token.getLastSeenAt();
        return lastSeenAt == null || !lastSeenAt.isAfter(now.minus(properties.getHeartbeatWriteInterval()));
    }

    static List<TableSessionToken> markTokensToRevokeForNewSlot(
            List<TableSessionToken> activeTokens, TableSessionProperties properties, LocalDateTime now) {
        List<TableSessionToken> validTokens = new ArrayList<>();
        List<TableSessionToken> tokensToRevoke = new ArrayList<>();

        for (TableSessionToken token : activeTokens) {
            if (isTokenExpired(token, properties, now)) {
                token.setRevokedAt(now);
                tokensToRevoke.add(token);
            } else {
                validTokens.add(token);
            }
        }

        int overflow = validTokens.size() - properties.getMaxActiveTokensPerSession() + 1;
        for (int index = 0; index < overflow; index++) {
            TableSessionToken token = validTokens.get(index);
            token.setRevokedAt(now);
            tokensToRevoke.add(token);
        }

        return tokensToRevoke;
    }
}
