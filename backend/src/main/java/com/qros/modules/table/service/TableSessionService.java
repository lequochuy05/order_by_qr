package com.qros.modules.table.service;

import com.qros.modules.table.config.TableSessionProperties;
import com.qros.modules.table.dto.request.TableSessionHeartbeatRequest;
import com.qros.modules.table.dto.response.TableSessionStartResponse;
import com.qros.modules.table.dto.response.TableSessionStateResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionTable;
import com.qros.modules.table.model.TableSessionToken;
import com.qros.modules.table.model.enums.TableSessionSource;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.repository.TableSessionTableRepository;
import com.qros.modules.table.repository.TableSessionTokenRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.TableChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TableSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final DiningTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final TableSessionTableRepository membershipRepository;
    private final TableSessionTokenRepository tokenRepository;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final TableActiveOrderChecker activeOrderChecker;
    private final TableSessionProperties tableSessionProperties;
    private final TransactionSideEffectService sideEffects;
    private final TableQrSecurityService tableQrSecurityService;

    public TableSessionService(
            DiningTableRepository tableRepository,
            TableSessionRepository sessionRepository,
            TableSessionTableRepository membershipRepository,
            TableSessionTokenRepository tokenRepository,
            CacheManager cacheManager,
            ApplicationEventPublisher eventPublisher,
            TableActiveOrderChecker activeOrderChecker,
            TableSessionProperties tableSessionProperties,
            TransactionSideEffectService sideEffects) {
        this(
                tableRepository,
                sessionRepository,
                membershipRepository,
                tokenRepository,
                cacheManager,
                eventPublisher,
                activeOrderChecker,
                tableSessionProperties,
                sideEffects,
                null);
    }

    @Autowired
    public TableSessionService(
            DiningTableRepository tableRepository,
            TableSessionRepository sessionRepository,
            TableSessionTableRepository membershipRepository,
            TableSessionTokenRepository tokenRepository,
            CacheManager cacheManager,
            ApplicationEventPublisher eventPublisher,
            TableActiveOrderChecker activeOrderChecker,
            TableSessionProperties tableSessionProperties,
            TransactionSideEffectService sideEffects,
            TableQrSecurityService tableQrSecurityService) {
        this.tableRepository = tableRepository;
        this.sessionRepository = sessionRepository;
        this.membershipRepository = membershipRepository;
        this.tokenRepository = tokenRepository;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
        this.activeOrderChecker = activeOrderChecker;
        this.tableSessionProperties = tableSessionProperties;
        this.sideEffects = sideEffects;
        this.tableQrSecurityService = tableQrSecurityService;
    }

    @Transactional(readOnly = true)
    public TableSessionStateResponse getPublicState(@NonNull String tableCode) {
        DiningTable table = tableRepository
                .findByTableCode(tableCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));

        boolean tableActive = table.getStatus() != TableStatus.INACTIVE;
        TableSession session = membershipRepository
                .findActiveOpenMembershipByTableId(table.getId(), TableSessionStatus.OPEN)
                .map(TableSessionTable::getSession)
                .orElse(null);
        List<DiningTable> memberTables = session == null ? List.of() : getActiveMemberTables(session.getId());

        return new TableSessionStateResponse(
                table.getTableCode(),
                table.getTableNumber(),
                table.getStatus().name(),
                session != null,
                tableActive,
                tableActive && session != null,
                session != null ? session.getId() : null,
                session != null && session.getTable() != null
                        ? session.getTable().getId()
                        : null,
                memberTables.stream().map(DiningTable::getId).toList(),
                memberTables.stream().map(DiningTable::getTableNumber).toList(),
                displayName(memberTables),
                session != null ? session.getGuestCount() : null,
                totalCapacity(memberTables),
                memberTables.size() > 1);
    }

    @Transactional
    public TableSessionStartResponse startPublicSession(@NonNull String tableCode) {
        return startPublicSession(tableCode, null);
    }

    @Transactional
    public TableSessionStartResponse startPublicSession(@NonNull String tableCode, String signature) {
        if (tableQrSecurityService != null) {
            tableQrSecurityService.validateSignature(tableCode, signature);
        }

        DiningTable table = tableRepository
                .findByTableCodeForUpdate(tableCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));

        if (table.getStatus() == TableStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "This table is inactive and cannot accept orders");
        }

        TableSession session = membershipRepository
                .findActiveOpenMembershipByTableIdForUpdate(table.getId(), TableSessionStatus.OPEN)
                .map(TableSessionTable::getSession)
                .orElse(null);

        if (session == null) {
            session = createOpenSession(table, TableSessionSource.CUSTOMER);
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        activeOrderChecker.attachActiveOrdersToSession(List.of(table.getId()), session);
        evictTableSessionCachesForTables(getActiveMemberTables(session.getId()));

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

    @Transactional
    public TableSession getOrCreateStaffSession(@NonNull Long tableId) {
        DiningTable table = tableRepository
                .findByIdForUpdate(tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND, "Table ID invalid"));

        if (table.getStatus() == TableStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "This table is inactive and cannot accept orders");
        }

        TableSession session = membershipRepository
                .findActiveOpenMembershipByTableIdForUpdate(table.getId(), TableSessionStatus.OPEN)
                .map(TableSessionTable::getSession)
                .orElse(null);

        if (session == null) {
            session = createOpenSession(table, TableSessionSource.STAFF);
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        evictTableSessionCachesForTables(getActiveMemberTables(session.getId()));
        eventPublisher.publishEvent(new TableChangeEvent());
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

    @Transactional
    public void requestPayment(@NonNull String tableCode, @NonNull String sessionToken) {
        TableSession session = requireOpenSession(sessionToken);
        validateSessionBelongsToTable(session, tableCode);

        List<DiningTable> memberTables = lockActiveMemberTables(session.getId());
        List<DiningTable> changed = memberTables.stream()
                .filter(table -> table.getStatus() == TableStatus.OCCUPIED)
                .peek(table -> table.setStatus(TableStatus.WAITING_FOR_PAYMENT))
                .toList();

        if (!changed.isEmpty()) {
            tableRepository.saveAll(changed);
            evictTableSessionCachesForTables(changed);
            eventPublisher.publishEvent(new TableChangeEvent());
        }
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

        TableSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));
        List<DiningTable> tables = lockActiveMemberTables(sessionId);
        List<TableSessionTable> memberships = membershipRepository.findActiveBySessionIdForUpdate(sessionId);

        LocalDateTime now = AppTime.now();
        if (session.isOpen()) {
            session.setStatus(normalizedStatus);
            session.setClosedAt(now);
            session.setClosedReason(reason);
            sessionRepository.save(session);
        }

        tokenRepository.revokeActiveTokensBySessionId(sessionId, now);
        memberships.forEach(membership -> membership.setReleasedAt(now));
        membershipRepository.saveAll(memberships);
        tables.forEach(this::releaseTableIfUnused);

        evictTableSessionCachesForTables(tables);
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
        List<DiningTable> changedTables = new ArrayList<>();
        for (TableSession session : staleSessions) {
            List<DiningTable> memberTables = lockActiveMemberTables(session.getId());
            List<TableSessionTable> memberships = membershipRepository.findActiveBySessionIdForUpdate(session.getId());

            session.setStatus(TableSessionStatus.EXPIRED);
            session.setClosedAt(now);
            session.setClosedReason("Expired before first order");
            tokenRepository.revokeActiveTokensBySessionId(session.getId(), now);

            memberships.forEach(membership -> membership.setReleasedAt(now));
            membershipRepository.saveAll(memberships);

            for (DiningTable table : memberTables) {
                if (table != null
                        && table.getStatus() != TableStatus.INACTIVE
                        && !activeOrderChecker.hasActiveOrders(table.getId())) {
                    table.setStatus(TableStatus.AVAILABLE);
                    tableRepository.save(table);
                    changedTables.add(table);
                }
            }
        }

        sessionRepository.saveAll(staleSessions);
        evictTableSessionCachesForTables(changedTables);
        eventPublisher.publishEvent(new TableChangeEvent());
        log.info("Expired {} stale table sessions without orders", staleSessions.size());
    }

    public void evictTableSessionCachesForTables(List<DiningTable> tables) {
        List<String> tableCodes = tables == null
                ? List.of()
                : tables.stream()
                        .filter(table -> table != null && table.getTableCode() != null)
                        .map(DiningTable::getTableCode)
                        .distinct()
                        .toList();
        sideEffects.afterCommit(
                () -> evictTableSessionCaches(tableCodes), "evict table-session caches after transaction commit");
    }

    private void evictTableSessionCaches(List<String> tableCodes) {
        evict(CacheNames.TABLES, "all_sorted");
        clear(CacheNames.STATS_DASHBOARD);

        tableCodes.forEach(tableCode -> {
            evict(CacheNames.TABLES, "code_" + tableCode);
            evict(CacheNames.TABLES, "public_code_" + tableCode);
        });
    }

    private List<DiningTable> lockActiveMemberTables(Long sessionId) {
        List<Long> tableIds = membershipRepository.findActiveTableIdsBySessionId(sessionId);
        if (tableIds.isEmpty()) {
            return List.of();
        }

        List<DiningTable> tables = tableRepository.findAllByIdInForUpdate(tableIds);
        if (tables.size() != tableIds.size()) {
            throw new BusinessException(
                    ErrorCode.CONCURRENT_MODIFICATION,
                    "Table membership changed while the session was being updated. Refresh and try again.");
        }
        return tables;
    }

    private TableSessionToken requireActiveToken(String sessionToken) {
        String tokenHash = hashToken(sessionToken);
        TableSessionToken token = tokenRepository
                .findFirstByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_INVALID));
        validateTokenLifetime(token, AppTime.now());
        return token;
    }

    private TableSession createOpenSession(DiningTable table, TableSessionSource source) {
        LocalDateTime now = AppTime.now();
        TableSession session = TableSession.builder()
                .table(table)
                .status(TableSessionStatus.OPEN)
                .openedAt(now)
                .lastActivityAt(now)
                .createdSource(source)
                .build();

        TableSession saved = sessionRepository.save(session);
        membershipRepository.save(TableSessionTable.builder()
                .session(saved)
                .table(table)
                .primaryTable(true)
                .joinedAt(now)
                .build());
        return saved;
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

    private TableSessionStartResponse toStartResponse(
            DiningTable requestedTable, TableSession session, String rawToken) {
        List<DiningTable> memberTables = getActiveMemberTables(session.getId());
        DiningTable primaryTable = session.getTable();

        return new TableSessionStartResponse(
                requestedTable.getTableCode(),
                requestedTable.getTableNumber(),
                requestedTable.getStatus().name(),
                session.getId(),
                rawToken,
                session.getStatus().name(),
                true,
                primaryTable != null ? primaryTable.getId() : null,
                memberTables.stream().map(DiningTable::getId).toList(),
                memberTables.stream().map(DiningTable::getTableNumber).toList(),
                displayName(memberTables),
                session.getGuestCount(),
                totalCapacity(memberTables),
                memberTables.size() > 1);
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

        List<DiningTable> memberTables = getActiveMemberTables(session.getId());
        if (memberTables.isEmpty()) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_MEMBERSHIP_NOT_FOUND);
        }

        if (memberTables.stream().anyMatch(table -> table.getStatus() == TableStatus.INACTIVE)) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "This table is inactive and cannot accept orders");
        }
    }

    private void validateSessionBelongsToTable(TableSession session, String tableCode) {
        if (!membershipRepository.existsActiveBySessionIdAndTableCode(session.getId(), tableCode)) {
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
        if (table == null
                || table.getStatus() == TableStatus.INACTIVE
                || table.getStatus() == TableStatus.AVAILABLE
                || activeOrderChecker.hasActiveOrders(table.getId())) {
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

    private List<DiningTable> getActiveMemberTables(Long sessionId) {
        if (sessionId == null) {
            return List.of();
        }

        return membershipRepository.findActiveBySessionId(sessionId).stream()
                .map(TableSessionTable::getTable)
                .sorted(Comparator.comparing(DiningTable::getTableNumber, TableSessionService::compareTableNumbers))
                .toList();
    }

    private String displayName(List<DiningTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return "Table";
        }

        return "Table "
                + tables.stream()
                        .map(DiningTable::getTableNumber)
                        .reduce((left, right) -> left + " + " + right)
                        .orElse("");
    }

    private int totalCapacity(List<DiningTable> tables) {
        if (tables == null) {
            return 0;
        }

        return tables.stream()
                .map(DiningTable::getCapacity)
                .filter(java.util.Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private static int compareTableNumbers(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }

        try {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } catch (NumberFormatException ignored) {
            return left.compareToIgnoreCase(right);
        }
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
