package com.qros.modules.table.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final DiningTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final TableSessionTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TableActiveOrderChecker activeOrderChecker;

    @Value("${table-session.no-order-expire-minutes:30}")
    private long noOrderExpireMinutes;

    @Transactional(readOnly = true)
    public TableSessionStateResponse getPublicState(@NonNull String tableCode) {
        DiningTable table = tableRepository.findByTableCode(tableCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));

        boolean tableActive = table.getStatus() != TableStatus.INACTIVE;
        boolean hasOpenSession = sessionRepository
                .findFirstByTableIdAndStatusOrderByOpenedAtDesc(
                        table.getId(),
                        TableSessionStatus.OPEN)
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
    @CacheEvict(value = { CacheNames.TABLES, CacheNames.STATS_DASHBOARD }, allEntries = true)
    public TableSessionStartResponse startPublicSession(@NonNull String tableCode) {
        DiningTable table = tableRepository.findByTableCodeForUpdate(tableCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));

        if (table.getStatus() == TableStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID,
                    "This table is inactive and cannot accept orders");
        }

        TableSession session = sessionRepository
                .findByTableIdAndStatusForUpdate(table.getId(), TableSessionStatus.OPEN)
                .orElse(null);

        if (session == null) {
            session = createOpenSession(table);
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        String rawToken = issueClientToken(session);
        eventPublisher.publishEvent(new TableChangeEvent());

        return toStartResponse(table, session, rawToken);
    }

    @Transactional
    public void heartbeat(@NonNull TableSessionHeartbeatRequest request) {
        TableSessionToken token = requireActiveToken(request.sessionToken());
        TableSession session = token.getSession();

        if (session == null || !session.isOpen()) {
            return;
        }

        validateOpenSession(session);

        LocalDateTime now = AppTime.now();
        token.setLastSeenAt(now);
        tokenRepository.save(token);

        sessionRepository.touchOpenSessionActivity(session.getId(), now);
    }

    @Transactional
    public TableSession requireOpenSessionForOrdering(
            @NonNull String tableCode,
            @NonNull String sessionToken) {
        TableSession session = requireOpenSession(sessionToken);

        validateSessionBelongsToTable(session, tableCode);

        touch(session);
        return session;
    }

    @Transactional(readOnly = true)
    public TableSession requireOpenSessionForRead(
            @NonNull String tableCode,
            @NonNull String sessionToken) {
        TableSession session = requireSessionForRead(tableCode, sessionToken);
        validateOpenSession(session);

        return session;
    }

    @Transactional(readOnly = true)
    public TableSession requireSessionForRead(
            @NonNull String tableCode,
            @NonNull String sessionToken) {
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

        TableSession session = sessionRepository.findByIdForUpdate(token.getSession().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));

        validateOpenSession(session);

        LocalDateTime now = AppTime.now();
        token.setLastSeenAt(now);
        tokenRepository.save(token);

        return session;
    }

    private TableSessionToken requireActiveToken(String sessionToken) {
        String tokenHash = hashToken(sessionToken);
        return tokenRepository
                .findFirstByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_INVALID));
    }

    @Transactional
    @CacheEvict(value = { CacheNames.TABLES, CacheNames.STATS_DASHBOARD }, allEntries = true)
    public void closeSession(Long sessionId, TableSessionStatus targetStatus, String reason) {
        if (sessionId == null) {
            return;
        }

        TableSessionStatus normalizedStatus = targetStatus == null
                ? TableSessionStatus.CLOSED
                : targetStatus;

        if (normalizedStatus == TableSessionStatus.OPEN) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID,
                    "Cannot close session with OPEN status");
        }

        TableSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));

        if (!session.isOpen()) {
            return;
        }

        session.setStatus(normalizedStatus);
        session.setClosedAt(AppTime.now());
        session.setClosedReason(reason);
        sessionRepository.save(session);

        eventPublisher.publishEvent(new TableChangeEvent());
    }

    @Scheduled(fixedDelayString = "${table-session.cleanup-fixed-delay-ms:300000}")
    @Transactional
    @CacheEvict(value = { CacheNames.TABLES, CacheNames.STATS_DASHBOARD }, allEntries = true)
    public void expireStaleOpenSessionsWithoutOrders() {
        LocalDateTime cutoff = AppTime.now().minusMinutes(noOrderExpireMinutes);
        List<TableSession> staleSessions = sessionRepository
                .findStaleOpenSessionsWithoutOrdersForUpdate(cutoff);

        if (staleSessions.isEmpty()) {
            return;
        }

        LocalDateTime now = AppTime.now();
        for (TableSession session : staleSessions) {
            session.setStatus(TableSessionStatus.EXPIRED);
            session.setClosedAt(now);
            session.setClosedReason("Expired before first order");

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
        String rawToken = generateRawToken();
        TableSessionToken token = TableSessionToken.builder()
                .session(session)
                .tokenHash(hashToken(rawToken))
                .issuedAt(AppTime.now())
                .lastSeenAt(AppTime.now())
                .build();

        tokenRepository.save(token);
        return rawToken;
    }

    private TableSessionStartResponse toStartResponse(
            DiningTable table,
            TableSession session,
            String rawToken) {
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
                    ErrorCode.TABLE_SESSION_INVALID,
                    "This table is inactive and cannot accept orders");
        }
    }

    private void validateSessionBelongsToTable(TableSession session, String tableCode) {
        if (session.getTable() == null
                || !Objects.equals(session.getTable().getTableCode(), tableCode)) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID,
                    "Session token does not belong to this table");
        }
    }

    private void touch(TableSession session) {
        if (session == null || session.getId() == null) {
            return;
        }

        sessionRepository.touchOpenSessionActivity(session.getId(), AppTime.now());
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
