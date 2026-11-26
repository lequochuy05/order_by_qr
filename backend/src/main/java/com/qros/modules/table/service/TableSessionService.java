package com.qros.modules.table.service;

import com.qros.modules.table.config.TableSessionProperties;
import com.qros.modules.table.dto.request.TableSessionHeartbeatRequest;
import com.qros.modules.table.dto.response.TableSessionStartResponse;
import com.qros.modules.table.dto.response.TableSessionStateResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionTable;
import com.qros.modules.table.model.TableSessionToken;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.model.enums.TableSessionSource;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableServiceRequestRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.repository.TableSessionTableRepository;
import com.qros.modules.table.repository.TableSessionTokenRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.ServiceRequestChangeEvent;
import com.qros.shared.event.DomainEvents.TableChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TableSessionService {

    private static final List<ServiceRequestStatus> OPEN_SERVICE_REQUEST_STATUSES =
            List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.CLAIMED);

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
    private final TableServiceRequestRepository serviceRequestRepository;

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
                null,
                null);
    }

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
                tableQrSecurityService,
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
            TableQrSecurityService tableQrSecurityService,
            TableServiceRequestRepository serviceRequestRepository) {
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
        this.serviceRequestRepository = serviceRequestRepository;
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
                .findActiveOpenMembershipByTableId(table.getId(), TableSessionStatus.OPEN)
                .map(TableSessionTable::getSession)
                .flatMap(observed -> sessionRepository.findByIdForUpdate(observed.getId()))
                .filter(TableSession::isOpen)
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
        if (!TableSessionTokenSupport.shouldWriteHeartbeat(token, tableSessionProperties, now)) {
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
                .findActiveOpenMembershipByTableId(table.getId(), TableSessionStatus.OPEN)
                .map(TableSessionTable::getSession)
                .flatMap(observed -> sessionRepository.findByIdForUpdate(observed.getId()))
                .filter(TableSession::isOpen)
                .orElse(null);

        if (session != null) {
            List<TableSessionTable> activeMemberships =
                    membershipRepository.findActiveBySessionIdForUpdate(session.getId());
            boolean stillMember = activeMemberships.stream()
                    .anyMatch(m -> m.getTable() != null
                            && table.getId().equals(m.getTable().getId()));
            if (!stillMember) {
                session = null;
            }
        }

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
    public boolean tableEverBelongedToSession(@NonNull Long sessionId, @NonNull String tableCode) {
        return membershipRepository.existsAnyBySessionIdAndTableCode(sessionId, tableCode);
    }

    @Transactional(readOnly = true)
    public boolean verifySessionAccessForOrder(
            @NonNull Long sessionId, @NonNull String tableCode, @NonNull String sessionToken) {
        if (sessionToken.isBlank() || tableCode.isBlank()) {
            return false;
        }
        String tokenHash = TableSessionTokenSupport.hashToken(sessionToken);
        return tokenRepository.existsBySession_IdAndTokenHash(sessionId, tokenHash)
                && membershipRepository.existsAnyBySessionIdAndTableCode(sessionId, tableCode);
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
        if (TableSessionTokenSupport.shouldWriteHeartbeat(token, tableSessionProperties, now)) {
            token.setLastSeenAt(now);
            tokenRepository.save(token);
        }

        return session;
    }

    @Transactional
    public void requestPayment(@NonNull String tableCode, @NonNull String sessionToken) {
        TableSessionToken token = requireActiveToken(sessionToken);
        Long sessionId = token.getSession() != null ? token.getSession().getId() : null;
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND);
        }

        // Canonical lock order: tables (id ASC) -> table_sessions -> table_session_tables
        List<DiningTable> memberTables = lockActiveMemberTables(sessionId);
        TableSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));
        validateOpenSession(session);
        validateSessionBelongsToTable(session, tableCode);

        LocalDateTime now = AppTime.now();
        if (TableSessionTokenSupport.shouldWriteHeartbeat(token, tableSessionProperties, now)) {
            token.setLastSeenAt(now);
            tokenRepository.save(token);
        }

        activeOrderChecker.validateAndPrepareSessionForPaymentRequest(session.getId());

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
    public void closeEmptySession(@NonNull Long sessionId, String actorEmail) {
        // Canonical lock order: tables (id ASC) -> table_sessions -> table_session_tables
        List<DiningTable> tables = lockActiveMemberTables(sessionId);
        TableSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));

        if (!session.isOpen()) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID, "Table session is already closed");
        }

        if (activeOrderChecker.hasActiveOrdersForSession(sessionId)
                || activeOrderChecker.hasPaymentInProgressForSession(sessionId)) {
            throw new BusinessException(
                    ErrorCode.TABLE_IN_USE, "Cannot close session while the table has an active order");
        }

        String actor = (actorEmail != null && !actorEmail.isBlank()) ? actorEmail.trim() : "staff";
        completeCloseLockedSession(session, tables, TableSessionStatus.CANCELLED, "Empty session closed by " + actor);
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

        // Canonical lock order: tables (id ASC) -> table_sessions -> table_session_tables
        List<DiningTable> tables = lockActiveMemberTables(sessionId);
        TableSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));
        completeCloseLockedSession(session, tables, normalizedStatus, reason);
    }

    private void completeCloseLockedSession(
            TableSession session, List<DiningTable> tables, TableSessionStatus normalizedStatus, String reason) {
        Long sessionId = session.getId();
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
        releaseUnusedTables(tables);

        int closedRequests = 0;
        if (serviceRequestRepository != null) {
            closedRequests = serviceRequestRepository.closeOpenRequestsBySessionId(
                    sessionId,
                    ServiceRequestStatus.CANCELLED,
                    OPEN_SERVICE_REQUEST_STATUSES,
                    "SYSTEM",
                    "Table session closed",
                    now);
        }

        evictTableSessionCachesForTables(tables);
        eventPublisher.publishEvent(new TableChangeEvent());
        if (closedRequests > 0) {
            Long tableId = session.getTable() != null ? session.getTable().getId() : null;
            eventPublisher.publishEvent(new ServiceRequestChangeEvent("AUTO_CLOSED", null, tableId));
        }
    }

    @Transactional
    public void expireStaleOpenSessionsWithoutOrders() {
        LocalDateTime cutoff = AppTime.now().minus(tableSessionProperties.getNoOrderExpire());
        List<Long> staleSessionIds = sessionRepository.findStaleOpenSessionIdsWithoutOrders(cutoff);

        if (staleSessionIds.isEmpty()) {
            return;
        }

        LocalDateTime now = AppTime.now();
        List<TableSession> expiredSessions = new ArrayList<>();
        List<TableSessionTable> membershipsToRelease = new ArrayList<>();
        Map<Long, DiningTable> candidateTablesById = new LinkedHashMap<>();
        boolean autoClosedRequests = false;
        for (Long sessionId : staleSessionIds) {
            // Canonical lock order: tables (id ASC) -> table_sessions -> table_session_tables
            List<DiningTable> memberTables = lockActiveMemberTables(sessionId);
            TableSession session =
                    sessionRepository.findByIdForUpdate(sessionId).orElse(null);
            if (session == null || !session.isOpen()) {
                continue;
            }
            if (session.getLastActivityAt() != null
                    && !session.getLastActivityAt().isBefore(cutoff)) {
                continue;
            }
            if (activeOrderChecker.hasActiveOrdersForSession(sessionId)
                    || activeOrderChecker.hasPaymentInProgressForSession(sessionId)) {
                continue;
            }

            List<TableSessionTable> memberships = membershipRepository.findActiveBySessionIdForUpdate(sessionId);

            session.setStatus(TableSessionStatus.EXPIRED);
            session.setClosedAt(now);
            session.setClosedReason("Expired before first order");
            expiredSessions.add(session);
            tokenRepository.revokeActiveTokensBySessionId(sessionId, now);

            memberships.forEach(membership -> membership.setReleasedAt(now));
            membershipsToRelease.addAll(memberships);

            for (DiningTable table : memberTables) {
                if (table != null && table.getId() != null) {
                    candidateTablesById.putIfAbsent(table.getId(), table);
                }
            }

            if (serviceRequestRepository != null) {
                int closed = serviceRequestRepository.closeOpenRequestsBySessionId(
                        sessionId,
                        ServiceRequestStatus.CANCELLED,
                        OPEN_SERVICE_REQUEST_STATUSES,
                        "SYSTEM",
                        "Table session expired",
                        now);
                if (closed > 0) {
                    autoClosedRequests = true;
                }
            }
        }

        if (expiredSessions.isEmpty()) {
            return;
        }

        if (!membershipsToRelease.isEmpty()) {
            membershipRepository.saveAll(membershipsToRelease);
        }
        List<DiningTable> changedTables = releaseUnusedTables(new ArrayList<>(candidateTablesById.values()));

        sessionRepository.saveAll(expiredSessions);
        evictTableSessionCachesForTables(changedTables);
        eventPublisher.publishEvent(new TableChangeEvent());
        if (autoClosedRequests) {
            eventPublisher.publishEvent(new ServiceRequestChangeEvent("AUTO_CLOSED", null, null));
        }
        log.info("Expired {} stale table sessions without orders", expiredSessions.size());
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
        String tokenHash = TableSessionTokenSupport.hashToken(sessionToken);
        TableSessionToken token = tokenRepository
                .findFirstByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_INVALID));
        TableSessionTokenSupport.validateTokenLifetime(token, tableSessionProperties, AppTime.now());
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

        String rawToken = TableSessionTokenSupport.generateRawToken();
        TableSessionToken token = TableSessionToken.builder()
                .session(session)
                .tokenHash(TableSessionTokenSupport.hashToken(rawToken))
                .issuedAt(now)
                .lastSeenAt(now)
                .build();

        tokenRepository.save(token);
        return rawToken;
    }

    private void reserveTokenSlot(TableSession session, LocalDateTime now) {
        List<TableSessionToken> activeTokens = tokenRepository.findActiveBySessionIdForUpdate(session.getId());
        List<TableSessionToken> tokensToRevoke =
                TableSessionTokenSupport.markTokensToRevokeForNewSlot(activeTokens, tableSessionProperties, now);
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

    private List<DiningTable> releaseUnusedTables(List<DiningTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return List.of();
        }

        List<DiningTable> candidates = tables.stream()
                .filter(table -> table != null
                        && table.getId() != null
                        && table.getStatus() != TableStatus.INACTIVE
                        && table.getStatus() != TableStatus.AVAILABLE)
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<Long> busyTableIds;
        if (candidates.size() == 1) {
            Long singleId = candidates.get(0).getId();
            busyTableIds = activeOrderChecker.hasActiveOrders(singleId) ? Set.of(singleId) : Set.of();
        } else {
            List<Long> candidateIds =
                    candidates.stream().map(DiningTable::getId).toList();
            busyTableIds = activeOrderChecker.findTableIdsWithActiveOrders(candidateIds);
        }

        List<DiningTable> toRelease = candidates.stream()
                .filter(table -> !busyTableIds.contains(table.getId()))
                .peek(table -> table.setStatus(TableStatus.AVAILABLE))
                .toList();
        if (!toRelease.isEmpty()) {
            tableRepository.saveAll(toRelease);
        }
        return toRelease;
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
                .sorted(Comparator.comparing(DiningTable::getTableNumber, TableNumberComparator::compare))
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

    private void evict(String cacheName, Object key) {
        com.qros.shared.cache.CacheHelper.evict(cacheManager, cacheName, key);
    }

    private void clear(String cacheName) {
        com.qros.shared.cache.CacheHelper.clear(cacheManager, cacheName);
    }
}
