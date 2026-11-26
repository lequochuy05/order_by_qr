package com.qros.modules.table.service;

import com.qros.modules.table.dto.request.TableMergeRequest;
import com.qros.modules.table.dto.response.TableMergeResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionTable;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.model.enums.TableSessionSource;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableServiceRequestRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.repository.TableSessionTableRepository;
import com.qros.modules.table.repository.TableSessionTokenRepository;
import com.qros.shared.event.DomainEvents.ServiceRequestChangeEvent;
import com.qros.shared.event.DomainEvents.TableChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableMergeService {

    static final int MAX_MERGE_TABLES = 6;
    private static final List<ServiceRequestStatus> OPEN_SERVICE_REQUEST_STATUSES =
            List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.CLAIMED);

    private final DiningTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final TableSessionTableRepository membershipRepository;
    private final TableSessionResolver sessionResolver;
    private final TableSessionService tableSessionService;
    private final TableActiveOrderChecker activeOrderChecker;
    private final ApplicationEventPublisher eventPublisher;
    private final TableSessionTokenRepository tokenRepository;
    private final TableServiceRequestRepository serviceRequestRepository;

    public TableMergeService(
            DiningTableRepository tableRepository,
            TableSessionRepository sessionRepository,
            TableSessionTableRepository membershipRepository,
            TableSessionResolver sessionResolver,
            TableSessionService tableSessionService,
            TableActiveOrderChecker activeOrderChecker,
            ApplicationEventPublisher eventPublisher) {
        this(
                tableRepository,
                sessionRepository,
                membershipRepository,
                sessionResolver,
                tableSessionService,
                activeOrderChecker,
                eventPublisher,
                null,
                null);
    }

    public TableMergeService(
            DiningTableRepository tableRepository,
            TableSessionRepository sessionRepository,
            TableSessionTableRepository membershipRepository,
            TableSessionResolver sessionResolver,
            TableSessionService tableSessionService,
            TableActiveOrderChecker activeOrderChecker,
            ApplicationEventPublisher eventPublisher,
            TableSessionTokenRepository tokenRepository) {
        this(
                tableRepository,
                sessionRepository,
                membershipRepository,
                sessionResolver,
                tableSessionService,
                activeOrderChecker,
                eventPublisher,
                tokenRepository,
                null);
    }

    @Autowired
    public TableMergeService(
            DiningTableRepository tableRepository,
            TableSessionRepository sessionRepository,
            TableSessionTableRepository membershipRepository,
            TableSessionResolver sessionResolver,
            TableSessionService tableSessionService,
            TableActiveOrderChecker activeOrderChecker,
            ApplicationEventPublisher eventPublisher,
            TableSessionTokenRepository tokenRepository,
            TableServiceRequestRepository serviceRequestRepository) {
        this.tableRepository = tableRepository;
        this.sessionRepository = sessionRepository;
        this.membershipRepository = membershipRepository;
        this.sessionResolver = sessionResolver;
        this.tableSessionService = tableSessionService;
        this.activeOrderChecker = activeOrderChecker;
        this.eventPublisher = eventPublisher;
        this.tokenRepository = tokenRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional
    public TableMergeResponse merge(@NonNull TableMergeRequest request, Long userId) {
        List<Long> requestedIds = validateMergeRequest(request);
        LockedMergeContext context = lockMergeContext(requestedIds, request.primaryTableId());

        TableSession session = context.targetSession() != null
                ? context.targetSession()
                : createStaffSession(context.tablesById().get(request.primaryTableId()), request.guestCount(), userId);
        DiningTable primaryTable = resolvePrimaryTable(session, context, request.primaryTableId());

        absorbEmptySecondarySessions(context.sessionsToAbsorb(), session);

        List<TableSessionTable> existingMemberships =
                membershipRepository.findActiveBySessionIdForUpdate(session.getId());
        List<DiningTable> mergedTables =
                collectMergedTables(existingMemberships, context.allTableIds(), context.tablesById());

        verifyCapacityAndPaymentGuards(session, mergedTables, request.guestCount());
        persistMergedSessionAndTables(session, primaryTable, existingMemberships, mergedTables, request, userId);

        return toResponse(session, membershipRepository.findActiveBySessionId(session.getId()), null);
    }

    private List<Long> validateMergeRequest(TableMergeRequest request) {
        List<Long> requestedIds = distinctIds(request.tableIds());
        if (requestedIds.size() < 2 || !requestedIds.contains(request.primaryTableId())) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_REQUIRES_MULTIPLE_TABLES);
        }
        if (requestedIds.size() > MAX_MERGE_TABLES) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST, "Cannot merge more than " + MAX_MERGE_TABLES + " tables at once");
        }
        return requestedIds;
    }

    private LockedMergeContext lockMergeContext(List<Long> requestedIds, Long primaryTableId) {
        List<Long> observedSessionIds = new ArrayList<>(new LinkedHashSet<>(
                membershipRepository.findActiveOpenSessionIdsByTableIds(requestedIds, TableSessionStatus.OPEN)));

        List<Long> tableIdsToLock = new ArrayList<>(requestedIds);
        for (Long observedId : observedSessionIds) {
            tableIdsToLock.addAll(membershipRepository.findActiveTableIdsBySessionId(observedId));
        }

        // Canonical lock order: tables (id ASC) -> table_sessions (id ASC) -> table_session_tables
        List<Long> distinctTableIdsToLock =
                tableIdsToLock.stream().distinct().sorted().toList();
        List<DiningTable> lockedTables = tableRepository.findAllByIdInForUpdate(distinctTableIdsToLock);
        if (lockedTables.size() != distinctTableIdsToLock.size()) {
            throw new BusinessException(ErrorCode.TABLE_NOT_FOUND);
        }

        Map<Long, DiningTable> tablesById = new LinkedHashMap<>();
        lockedTables.forEach(table -> {
            if (table.getStatus() == TableStatus.INACTIVE) {
                throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID, "Inactive tables cannot be merged");
            }
            tablesById.put(table.getId(), table);
        });

        List<Long> currentSessionIds =
                membershipRepository.findActiveOpenSessionIdsByTableIds(requestedIds, TableSessionStatus.OPEN).stream()
                        .distinct()
                        .sorted()
                        .toList();

        if (observedSessionIds.isEmpty() && !currentSessionIds.isEmpty()) {
            throw concurrentMergeConflict();
        }
        if (!observedSessionIds.isEmpty()
                && !new LinkedHashSet<>(observedSessionIds).equals(new LinkedHashSet<>(currentSessionIds))) {
            throw concurrentMergeConflict();
        }

        List<TableSession> lockedSessions = new ArrayList<>();
        for (Long sessionId : currentSessionIds) {
            TableSession locked = sessionRepository
                    .findByIdForUpdate(sessionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));
            if (!locked.isOpen()) {
                throw concurrentMergeConflict();
            }
            lockedSessions.add(locked);
        }

        if (activeOrderChecker.countActiveOrdersByTableIds(distinctTableIdsToLock) > 1) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_HAS_MULTIPLE_ACTIVE_ORDERS);
        }

        List<TableSession> sessionsWithOrders = lockedSessions.stream()
                .filter(s -> activeOrderChecker.hasActiveOrdersForSession(s.getId())
                        || activeOrderChecker.hasPaymentInProgressForSession(s.getId()))
                .toList();
        if (sessionsWithOrders.size() > 1) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_HAS_MULTIPLE_ACTIVE_ORDERS);
        }

        TableSession targetSession = null;
        if (sessionsWithOrders.size() == 1) {
            targetSession = sessionsWithOrders.getFirst();
        } else if (!lockedSessions.isEmpty()) {
            targetSession = lockedSessions.stream()
                    .filter(s -> s.getTable() != null
                            && primaryTableId.equals(s.getTable().getId()))
                    .findFirst()
                    .orElse(lockedSessions.getFirst());
        }

        TableSession finalTarget = targetSession;
        List<TableSession> sessionsToAbsorb = lockedSessions.stream()
                .filter(s -> finalTarget == null || !s.getId().equals(finalTarget.getId()))
                .toList();

        return new LockedMergeContext(
                targetSession,
                sessionsToAbsorb,
                new LinkedHashSet<>(currentSessionIds),
                distinctTableIdsToLock,
                tablesById);
    }

    private void absorbEmptySecondarySessions(List<TableSession> sessionsToAbsorb, TableSession targetSession) {
        if (sessionsToAbsorb == null || sessionsToAbsorb.isEmpty()) {
            return;
        }
        LocalDateTime now = AppTime.now();
        for (TableSession secondary : sessionsToAbsorb) {
            if (activeOrderChecker.hasActiveOrdersForSession(secondary.getId())
                    || activeOrderChecker.hasPaymentInProgressForSession(secondary.getId())) {
                throw new BusinessException(ErrorCode.TABLE_MERGE_HAS_MULTIPLE_ACTIVE_ORDERS);
            }
            List<TableSessionTable> secondaryMemberships =
                    membershipRepository.findActiveBySessionIdForUpdate(secondary.getId());
            secondary.setStatus(TableSessionStatus.CANCELLED);
            secondary.setClosedAt(now);
            secondary.setClosedReason("Absorbed into merged session #" + targetSession.getId());
            sessionRepository.save(secondary);

            if (tokenRepository != null) {
                tokenRepository.revokeActiveTokensBySessionId(secondary.getId(), now);
            }
            if (serviceRequestRepository != null) {
                int closedRequests = serviceRequestRepository.closeOpenRequestsBySessionId(
                        secondary.getId(),
                        ServiceRequestStatus.CANCELLED,
                        OPEN_SERVICE_REQUEST_STATUSES,
                        "SYSTEM",
                        "Session absorbed into merged session #" + targetSession.getId(),
                        now);
                if (closedRequests > 0) {
                    Long secondaryTableId =
                            secondary.getTable() != null ? secondary.getTable().getId() : null;
                    eventPublisher.publishEvent(new ServiceRequestChangeEvent("AUTO_CLOSED", null, secondaryTableId));
                }
            }
            secondaryMemberships.forEach(m -> m.setReleasedAt(now));
            if (!secondaryMemberships.isEmpty()) {
                membershipRepository.saveAll(secondaryMemberships);
            }
        }
    }

    private DiningTable resolvePrimaryTable(TableSession session, LockedMergeContext context, Long primaryTableId) {
        DiningTable primaryTable = session.getTable();
        if (primaryTable == null) {
            primaryTable = context.tablesById().get(primaryTableId);
            session.setTable(primaryTable);
        } else if (!context.observedSessionIds().isEmpty()
                && !primaryTable.getId().equals(primaryTableId)) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "Cannot change primary table of an active session");
        }
        return primaryTable;
    }

    private List<DiningTable> collectMergedTables(
            List<TableSessionTable> existingMemberships, List<Long> requestedIds, Map<Long, DiningTable> tablesById) {
        Map<Long, DiningTable> mergedTablesById = new LinkedHashMap<>();
        existingMemberships.forEach(
                membership -> mergedTablesById.put(membership.getTable().getId(), membership.getTable()));
        requestedIds.forEach(id -> mergedTablesById.put(id, tablesById.get(id)));
        return mergedTablesById.values().stream()
                .sorted(Comparator.comparing(DiningTable::getId))
                .toList();
    }

    private void verifyCapacityAndPaymentGuards(
            TableSession session, List<DiningTable> mergedTables, Integer guestCount) {
        int totalCapacity = sessionResolver.totalCapacity(mergedTables);
        if (guestCount != null && guestCount > totalCapacity) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_CAPACITY_EXCEEDED);
        }
        if (activeOrderChecker.hasPaymentInProgressForSession(session.getId())) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_PAYMENT_IN_PROGRESS);
        }
    }

    private void persistMergedSessionAndTables(
            TableSession session,
            DiningTable primaryTable,
            List<TableSessionTable> existingMemberships,
            List<DiningTable> mergedTables,
            TableMergeRequest request,
            Long userId) {
        session.setGuestCount(request.guestCount());
        if (session.getCreatedByUserId() == null) {
            session.setCreatedByUserId(userId);
        }
        sessionRepository.save(session);

        Set<Long> existingTableIds = existingMemberships.stream()
                .map(membership -> membership.getTable().getId())
                .collect(Collectors.toSet());

        LocalDateTime now = AppTime.now();
        List<TableSessionTable> membershipsToCreate = new ArrayList<>();
        for (DiningTable table : mergedTables) {
            if (existingTableIds.contains(table.getId())) {
                continue;
            }
            membershipsToCreate.add(TableSessionTable.builder()
                    .session(session)
                    .table(table)
                    .primaryTable(table.getId().equals(primaryTable.getId()))
                    .joinedAt(now)
                    .joinedByUserId(userId)
                    .build());
        }
        if (!membershipsToCreate.isEmpty()) {
            membershipRepository.saveAll(membershipsToCreate);
        }

        for (DiningTable table : mergedTables) {
            if (table.getStatus() != TableStatus.OCCUPIED) {
                table.setStatus(TableStatus.OCCUPIED);
            }
        }
        tableRepository.saveAll(mergedTables);

        activeOrderChecker.attachActiveOrdersToSession(
                mergedTables.stream().map(DiningTable::getId).toList(), session);
        tableSessionService.evictTableSessionCachesForTables(mergedTables);
        eventPublisher.publishEvent(new TableChangeEvent());
    }

    private record LockedMergeContext(
            TableSession targetSession,
            List<TableSession> sessionsToAbsorb,
            Set<Long> observedSessionIds,
            List<Long> allTableIds,
            Map<Long, DiningTable> tablesById) {}

    @Transactional
    public TableMergeResponse cancelMerge(@NonNull Long sessionId) {
        // Canonical lock order: tables (id ASC) -> table_sessions -> table_session_tables
        List<Long> tableIds = membershipRepository.findActiveTableIdsBySessionId(sessionId);
        if (!tableIds.isEmpty()) {
            tableRepository.findAllByIdInForUpdate(tableIds);
        }

        TableSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));

        if (!session.isOpen()) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID, "Only open sessions can be cancelled");
        }

        List<TableSessionTable> memberships = membershipRepository.findActiveBySessionIdForUpdate(sessionId);
        if (memberships.size() <= 1) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "Only merged table sessions can cancel a merge");
        }

        if (activeOrderChecker.hasPaymentInProgressForSession(sessionId)) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_PAYMENT_IN_PROGRESS);
        }
        if (activeOrderChecker.hasBillableItemsForSession(sessionId)
                || activeOrderChecker.hasActiveOrdersForSession(sessionId)) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "Cannot cancel a merge after order items were added");
        }

        boolean preservePrimarySession = session.getCreatedSource() == TableSessionSource.CUSTOMER
                || (tokenRepository != null && tokenRepository.existsBySession_Id(sessionId));

        if (preservePrimarySession) {
            LocalDateTime now = AppTime.now();
            List<TableSessionTable> secondaryMemberships =
                    memberships.stream().filter(m -> !m.isPrimaryTable()).toList();
            List<DiningTable> detachedTables = new ArrayList<>();
            for (TableSessionTable membership : secondaryMemberships) {
                membership.setReleasedAt(now);
                DiningTable detached = membership.getTable();
                if (detached != null && detached.getStatus() != TableStatus.INACTIVE) {
                    detached.setStatus(TableStatus.AVAILABLE);
                    detachedTables.add(detached);
                }
            }
            if (!secondaryMemberships.isEmpty()) {
                membershipRepository.saveAll(secondaryMemberships);
            }
            if (!detachedTables.isEmpty()) {
                tableRepository.saveAll(detachedTables);
            }
            List<DiningTable> tablesToEvict = new ArrayList<>(detachedTables);
            if (session.getTable() != null) {
                tablesToEvict.add(session.getTable());
            }
            if (!tablesToEvict.isEmpty()) {
                tableSessionService.evictTableSessionCachesForTables(tablesToEvict);
            }
            eventPublisher.publishEvent(new TableChangeEvent());
            List<TableSessionTable> remaining = memberships.stream()
                    .filter(TableSessionTable::isPrimaryTable)
                    .toList();
            return toResponse(session, remaining, null);
        }

        tableSessionService.closeSession(sessionId, TableSessionStatus.CANCELLED, "Merge cancelled");

        return toResponse(session, memberships, null);
    }

    private TableSession createStaffSession(DiningTable primaryTable, Integer guestCount, Long userId) {
        LocalDateTime now = AppTime.now();
        TableSession session = TableSession.builder()
                .table(primaryTable)
                .status(TableSessionStatus.OPEN)
                .openedAt(now)
                .lastActivityAt(now)
                .createdSource(TableSessionSource.STAFF)
                .guestCount(guestCount)
                .createdByUserId(userId)
                .build();

        TableSession saved = sessionRepository.save(session);
        membershipRepository.save(TableSessionTable.builder()
                .session(saved)
                .table(primaryTable)
                .primaryTable(true)
                .joinedAt(now)
                .joinedByUserId(userId)
                .build());
        return saved;
    }

    private TableMergeResponse toResponse(
            TableSession session, List<TableSessionTable> memberships, Long activeOrderId) {
        List<DiningTable> tables = memberships.stream()
                .map(TableSessionTable::getTable)
                .sorted(Comparator.comparing(DiningTable::getTableNumber, TableMergeService::compareTableNumbers))
                .toList();
        DiningTable primaryTable = sessionResolver.requirePrimaryTable(session);

        return new TableMergeResponse(
                session.getId(),
                primaryTable.getId(),
                primaryTable.getTableNumber(),
                tables.stream().map(DiningTable::getId).toList(),
                tables.stream().map(DiningTable::getTableNumber).toList(),
                sessionResolver.displayName(tables),
                session.getGuestCount(),
                sessionResolver.totalCapacity(tables),
                session.getStatus().name(),
                activeOrderId);
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }

        return ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    private BusinessException concurrentMergeConflict() {
        return new BusinessException(
                ErrorCode.CONCURRENT_MODIFICATION,
                "The table session changed while the merge was waiting for locks. Refresh and try again.");
    }

    private static int compareTableNumbers(String left, String right) {
        return TableNumberComparator.compare(left, right);
    }
}
