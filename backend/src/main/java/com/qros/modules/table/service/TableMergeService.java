package com.qros.modules.table.service;

import com.qros.modules.table.dto.request.TableMergeRequest;
import com.qros.modules.table.dto.response.TableMergeResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionTable;
import com.qros.modules.table.model.enums.TableSessionSource;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.repository.TableSessionTableRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TableMergeService {

    static final int MAX_MERGE_TABLES = 6;

    private final DiningTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final TableSessionTableRepository membershipRepository;
    private final TableSessionResolver sessionResolver;
    private final TableSessionService tableSessionService;
    private final TableActiveOrderChecker activeOrderChecker;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TableMergeResponse merge(@NonNull TableMergeRequest request, Long userId) {
        List<Long> requestedIds = distinctIds(request.tableIds());
        if (requestedIds.size() < 2 || !requestedIds.contains(request.primaryTableId())) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_REQUIRES_MULTIPLE_TABLES);
        }
        if (requestedIds.size() > MAX_MERGE_TABLES) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST, "Cannot merge more than " + MAX_MERGE_TABLES + " tables at once");
        }

        List<Long> observedSessionIds =
                membershipRepository.findActiveOpenSessionIdsByTableIds(requestedIds, TableSessionStatus.OPEN);
        Set<Long> uniqueObservedSessionIds = new LinkedHashSet<>(observedSessionIds);
        if (uniqueObservedSessionIds.size() > 1) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_HAS_MULTIPLE_ACTIVE_ORDERS);
        }

        TableSession session = null;
        List<Long> tableIdsToLock = new ArrayList<>(requestedIds);
        if (!uniqueObservedSessionIds.isEmpty()) {
            Long sessionId = uniqueObservedSessionIds.iterator().next();
            session = sessionRepository
                    .findByIdForUpdate(sessionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));
            if (!session.isOpen()) {
                throw concurrentMergeConflict();
            }
            tableIdsToLock.addAll(membershipRepository.findActiveTableIdsBySessionId(sessionId));
        }

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

        Set<Long> currentSessionIds = new LinkedHashSet<>(
                membershipRepository.findActiveOpenSessionIdsByTableIds(requestedIds, TableSessionStatus.OPEN));
        if (currentSessionIds.size() > 1) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_HAS_MULTIPLE_ACTIVE_ORDERS);
        }
        if (session == null && !currentSessionIds.isEmpty()) {
            throw concurrentMergeConflict();
        }
        if (session != null
                && (currentSessionIds.isEmpty()
                        || !currentSessionIds.iterator().next().equals(session.getId()))) {
            throw concurrentMergeConflict();
        }

        int activeOrderCount = activeOrderChecker.countActiveOrdersByTableIds(requestedIds);
        if (activeOrderCount > 1) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_HAS_MULTIPLE_ACTIVE_ORDERS);
        }

        if (session == null) {
            session = createStaffSession(tablesById.get(request.primaryTableId()), request.guestCount(), userId);
        }

        DiningTable primaryTable = session.getTable();
        if (primaryTable == null) {
            primaryTable = tablesById.get(request.primaryTableId());
            session.setTable(primaryTable);
        } else if (!uniqueObservedSessionIds.isEmpty() && !primaryTable.getId().equals(request.primaryTableId())) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "Cannot change primary table of an active session");
        }

        List<TableSessionTable> existingMemberships =
                membershipRepository.findActiveBySessionIdForUpdate(session.getId());
        Map<Long, DiningTable> mergedTablesById = new LinkedHashMap<>();
        existingMemberships.forEach(
                membership -> mergedTablesById.put(membership.getTable().getId(), membership.getTable()));
        requestedIds.forEach(id -> mergedTablesById.put(id, tablesById.get(id)));
        List<DiningTable> mergedTables = mergedTablesById.values().stream()
                .sorted(Comparator.comparing(DiningTable::getId))
                .toList();

        int totalCapacity = sessionResolver.totalCapacity(mergedTables);
        if (request.guestCount() != null && request.guestCount() > totalCapacity) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_CAPACITY_EXCEEDED);
        }

        if (activeOrderChecker.hasPaymentInProgressForSession(session.getId())) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_PAYMENT_IN_PROGRESS);
        }

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

        return toResponse(session, membershipRepository.findActiveBySessionId(session.getId()), null);
    }

    @Transactional
    public TableMergeResponse cancelMerge(@NonNull Long sessionId) {
        TableSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_NOT_FOUND));

        if (!session.isOpen()) {
            throw new BusinessException(ErrorCode.TABLE_SESSION_INVALID, "Only open sessions can be cancelled");
        }

        if (activeOrderChecker.hasPaymentInProgressForSession(sessionId)) {
            throw new BusinessException(ErrorCode.TABLE_MERGE_PAYMENT_IN_PROGRESS);
        }
        if (activeOrderChecker.hasBillableItemsForSession(sessionId)) {
            throw new BusinessException(
                    ErrorCode.TABLE_SESSION_INVALID, "Cannot cancel a merge after order items were added");
        }

        List<TableSessionTable> memberships = membershipRepository.findActiveBySessionId(sessionId);

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
}
