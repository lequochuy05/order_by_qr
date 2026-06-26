package com.qros.modules.table.service;

import com.qros.modules.table.config.TableSessionProperties;
import com.qros.modules.table.dto.request.TableSessionHeartbeatRequest;
import com.qros.modules.table.dto.response.TableSessionStartResponse;
import com.qros.modules.table.dto.response.TableSessionStateResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionToken;
import com.qros.modules.table.model.enums.TableSessionSource;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.repository.TableSessionTokenRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.TableChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Lazy(false)
@RequiredArgsConstructor
public class TableSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final DiningTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final TableSessionTokenRepository tokenRepository;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final TableActiveOrderChecker activeOrderChecker;
    private final TableSessionProperties tableSessionProperties;

    @Transactional(readOnly = true)
    public TableSessionStateResponse getPublicState(@NonNull String tableCode) {
        DiningTable table = tableRepository
                .findByTableCode(tableCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));

        boolean tableActive = table.getStatus() != TableStatus.INACTIVE;
        boolean hasOpenSession = sessionRepository
                .findFirstByTableIdAndStatusOrderByOpenedAtDesc(table.getId(), TableSessionStatus.OPEN)
                .isPresent();

        return new TableSessionStateResponse(
                table.getTableCode(),
                table.getTableNumber(),
                table.getStatus().name(),
                hasOpenSession,
                tableActive,
                tableActive && hasOpenSession);
    }

    @Transactional
    public TableSessionStartResponse startPublicSession(@NonNull String tableCode) {
        DiningTable table = tableRepository
                .findByTableCodeForUpdate(tableCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));

        if (table.getStatus() == TableStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "This table is inactive and cannot accept orders");
        }

        TableSession session = sessionRepository
                .findByTableIdAndStatusForUpdate(table.getId(), TableSessionStatus.OPEN)
                .orElse(null);

        if (session == null) {
            session = createOpenSession(table);
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        activeOrderChecker.attachActiveOrderToSession(table.getId(), session);
        evictTableSessionCaches(table);

        String rawToken = issueClientToken(session);
        eventPublisher.publishEvent(new TableChangeEvent());

        return toStartResponse(table, session, rawToken);
    }

    @Transactional
    public void heartbeat(@NonNull TableSessionHeartbeatRequest request) {
        TableSessionToken token = requireActiveToken(request.sessionToken());
        TableSession session = token.getSession();

        if (session == null) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND);
        }

        validateOpenSession(session);

        LocalDateTime now = AppTime.now();
        if (!shouldWriteHeartbeat(token, now)) {
            return;
        }

        token.setLastSeenAt(now);
        tokenRepository.save(token);

        sessionRepository.touchOpenSessionActivity(session.getId(), now);
    }

    @Transactional
    public TableSession requireOpenSessionForOrdering(@NonNull String tableCode, @NonNull String sessionToken) {
        TableSession session = requireOpenSession(sessionToken);

        validateSessionBelongsToTable(session, tableCode);

        touchIfStale(session, AppTime.now());
        return session;
    }

    @Transactional(readOnly = true)
    public TableSession requireOpenSessionForRead(@NonNull String tableCode, @NonNull String sessionToken) {
        TableSession session = requireSessionForRead(tableCode, sessionToken);
        validateOpenSession(session);

        return session;
    }

    @Transactional(readOnly = true)
    public TableSession requireSessionForRead(@NonNull String tableCode, @NonNull String sessionToken) {
        TableSessionToken token = requireActiveToken(sessionToken);
        TableSession session = token.getSession();

        if (session == null) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND);
        }

        validateSessionBelongsToTable(session, tableCode);

        return session;
    }

    @Transactional
    public TableSession requireOpenSession(@NonNull String sessionToken) {
        TableSessionToken token = requireActiveToken(sessionToken);

        TableSession session = sessionRepository
                .findByIdForUpdate(token.getSession().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));

        validateOpenSession(session);

        LocalDateTime now = AppTime.now();
        if (shouldWriteHeartbeat(token, now)) {
            token.setLastSeenAt(now);
            tokenRepository.save(token);
        }

        return session;
    }

    private TableSessionToken requireActiveToken(String sessionToken) {
        String tokenHash = hashToken(sessionToken);
        TableSessionToken token = tokenRepository
                .findFirstByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_INVALID));
        validateTokenLifetime(token, AppTime.now());
        return token;
    }

    @Transactional
    public void closeSession(Long sessionId, TableSessionStatus targetStatus, String reason) {
        if (sessionId == null) {
            return;
        }

        TableSessionStatus normalizedStatus = targetStatus == null ? TableSessionStatus.CLOSED : targetStatus;

        if (normalizedStatus == TableSessionStatus.OPEN) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID, "Cannot close session with OPEN status");
        }

        Long tableId = sessionRepository
                .findTableIdById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));
        DiningTable table = tableRepository
                .findByIdForUpdate(tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));
        TableSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));

        LocalDateTime now = AppTime.now();
        if (session.isOpen()) {
            session.setStatus(normalizedStatus);
            session.setClosedAt(now);
            session.setClosedReason(reason);
            sessionRepository.save(session);
        }

        tokenRepository.revokeActiveTokensBySessionId(sessionId, now);
        releaseTableIfUnused(table);

        evictTableSessionCaches(table);
        eventPublisher.publishEvent(new TableChangeEvent());
    }

    @Scheduled(fixedDelayString = "${table-session.cleanup-fixed-delay-ms:300000}")
    @Transactional
    public void expireStaleOpenSessionsWithoutOrders() {
        LocalDateTime cutoff = AppTime.now().minus(tableSessionProperties.getNoOrderExpire());
        List<TableSession> staleSessions = sessionRepository.findStaleOpenSessionsWithoutOrdersForUpdate(cutoff);

        if (staleSessions.isEmpty()) {
            return;
        }

        LocalDateTime now = AppTime.now();
        for (TableSession session : staleSessions) {
            session.setStatus(TableSessionStatus.EXPIRED);
            session.setClosedAt(now);
            session.setClosedReason("Expired before first order");
            tokenRepository.revokeActiveTokensBySessionId(session.getId(), now);

            DiningTable table = session.getTable();
            if (table != null && table.getStatus() != TableStatus.INACTIVE) {
                boolean hasActiveOrders = activeOrderChecker.hasActiveOrders(table.getId());

                if (!hasActiveOrders) {
                    table.setStatus(TableStatus.AVAILABLE);
                    tableRepository.save(table);
                }
            }
        }

        sessionRepository.saveAll(staleSessions);
        staleSessions.forEach(session -> evictTableSessionCaches(session.getTable()));
        eventPublisher.publishEvent(new TableChangeEvent());
        log.info("Expired {} stale table sessions without orders", staleSessions.size());
    }

    private TableSession createOpenSession(DiningTable table) {
        LocalDateTime now = AppTime.now();
        TableSession session = TableSession.builder()
                .table(table)
                .status(TableSessionStatus.OPEN)
                .openedAt(now)
                .lastActivityAt(now)
                .createdSource(TableSessionSource.CUSTOMER)
                .build();

        return sessionRepository.save(session);
    }

    private String issueClientToken(TableSession session) {
        LocalDateTime now = AppTime.now();
        reserveTokenSlot(session, now);

        String rawToken = generateRawToken();
        TableSessionToken token = TableSessionToken.builder()
                .session(session)
                .tokenHash(hashToken(rawToken))
                .issuedAt(now)
                .lastSeenAt(now)
                .build();

        tokenRepository.save(token);
        return rawToken;
    }

    private void reserveTokenSlot(TableSession session, LocalDateTime now) {
        List<TableSessionToken> activeTokens = tokenRepository.findActiveBySessionIdForUpdate(session.getId());
        List<TableSessionToken> validTokens = new ArrayList<>();
        List<TableSessionToken> tokensToRevoke = new ArrayList<>();

        for (TableSessionToken token : activeTokens) {
            if (isTokenExpired(token, now)) {
                token.setRevokedAt(now);
                tokensToRevoke.add(token);
            } else {
                validTokens.add(token);
            }
        }

        int overflow = validTokens.size() - tableSessionProperties.getMaxActiveTokensPerSession() + 1;
        for (int index = 0; index < overflow; index++) {
            TableSessionToken token = validTokens.get(index);
            token.setRevokedAt(now);
            tokensToRevoke.add(token);
        }

        if (!tokensToRevoke.isEmpty()) {
            tokenRepository.saveAll(tokensToRevoke);
        }
    }

    private TableSessionStartResponse toStartResponse(DiningTable table, TableSession session, String rawToken) {
        return new TableSessionStartResponse(
                table.getTableCode(),
                table.getTableNumber(),
                table.getStatus().name(),
                session.getId(),
                rawToken,
                session.getStatus().name(),
                true);
    }

    private void validateOpenSession(TableSession session) {
        if (session.getStatus() == TableSessionStatus.EXPIRED
                || session.getStatus() == TableSessionStatus.CLOSED
                || session.getStatus() == TableSessionStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_EXPIRED);
        }

        if (!session.isOpen()) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID);
        }

        if (session.getTable() == null || session.getTable().getStatus() == TableStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "This table is inactive and cannot accept orders");
        }
    }

    private void validateSessionBelongsToTable(TableSession session, String tableCode) {
        if (session.getTable() == null || !Objects.equals(session.getTable().getTableCode(), tableCode)) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID, "Session token does not belong to this table");
        }
    }

    private void validateTokenLifetime(TableSessionToken token, LocalDateTime now) {
        if (token.getIssuedAt() == null) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID);
        }

        if (isTokenExpired(token, now)) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_EXPIRED);
        }
    }

    private boolean isTokenExpired(TableSessionToken token, LocalDateTime now) {
        LocalDateTime issuedAt = token.getIssuedAt();
        if (issuedAt == null) {
            return true;
        }

        LocalDateTime lastSeenAt = token.getLastSeenAt() != null ? token.getLastSeenAt() : issuedAt;
        return issuedAt.isBefore(now.minus(tableSessionProperties.getTokenMaxLifetime()))
                || lastSeenAt.isBefore(now.minus(tableSessionProperties.getTokenIdleTimeout()));
    }

    private boolean shouldWriteHeartbeat(TableSessionToken token, LocalDateTime now) {
        LocalDateTime lastSeenAt = token.getLastSeenAt();
        return lastSeenAt == null || !lastSeenAt.isAfter(now.minus(tableSessionProperties.getHeartbeatWriteInterval()));
    }

    private void releaseTableIfUnused(DiningTable table) {
        if (table.getStatus() == TableStatus.INACTIVE || activeOrderChecker.hasActiveOrders(table.getId())) {
            return;
        }

        table.setStatus(TableStatus.AVAILABLE);
        tableRepository.save(table);
    }

    private void touchIfStale(TableSession session, LocalDateTime now) {
        if (session == null || session.getId() == null) {
            return;
        }

        LocalDateTime lastActivityAt = session.getLastActivityAt();
        boolean shouldTouch = lastActivityAt == null
                || !lastActivityAt.isAfter(now.minus(tableSessionProperties.getHeartbeatWriteInterval()));

        if (shouldTouch) {
            sessionRepository.touchOpenSessionActivity(session.getId(), now);
            session.setLastActivityAt(now);
        }
    }

    private void evictTableSessionCaches(DiningTable table) {
        evict(CacheNames.TABLES, "all_sorted");
        clear(CacheNames.STATS_DASHBOARD);

        if (table == null || table.getTableCode() == null) {
            return;
        }

        evict(CacheNames.TABLES, "code_" + table.getTableCode());
        evict(CacheNames.TABLES, "public_code_" + table.getTableCode());
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(key);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
