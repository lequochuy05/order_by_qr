package com.qros.modules.table.service;

import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionTable;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionTableRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableSessionResolver {

    private final DiningTableRepository tableRepository;
    private final TableSessionTableRepository membershipRepository;

    public Optional<TableSession> findOpenSessionByTableId(@NonNull Long tableId) {
        return membershipRepository
                .findActiveOpenMembershipByTableId(tableId, TableSessionStatus.OPEN)
                .map(TableSessionTable::getSession);
    }

    public Optional<TableSession> findOpenSessionByTableIdForUpdate(@NonNull Long tableId) {
        return membershipRepository
                .findActiveOpenMembershipByTableIdForUpdate(tableId, TableSessionStatus.OPEN)
                .map(TableSessionTable::getSession);
    }

    public Optional<TableSession> findOpenSessionByTableCode(@NonNull String tableCode) {
        DiningTable table = tableRepository
                .findByTableCode(tableCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID));
        return findOpenSessionByTableId(table.getId());
    }

    public List<TableSessionTable> getActiveMemberships(@NonNull Long sessionId) {
        return membershipRepository.findActiveBySessionId(sessionId);
    }

    public List<TableSessionTable> findActiveBySessionStatus(@NonNull TableSessionStatus status) {
        return membershipRepository.findActiveBySessionStatus(status);
    }

    public List<DiningTable> getActiveMemberTables(@NonNull Long sessionId) {
        return getActiveMemberships(sessionId).stream()
                .map(TableSessionTable::getTable)
                .sorted(Comparator.comparing(DiningTable::getTableNumber, TableSessionResolver::compareTableNumbers))
                .toList();
    }

    public List<DiningTable> getAllMemberTables(@NonNull Long sessionId) {
        return membershipRepository.findAllBySessionId(sessionId).stream()
                .map(TableSessionTable::getTable)
                .distinct()
                .sorted(Comparator.comparing(DiningTable::getTableNumber, TableSessionResolver::compareTableNumbers))
                .toList();
    }

    public java.util.Map<Long, String> getDisplayNamesBySessionIds(java.util.Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return java.util.Map.of();
        }
        List<Long> distinctIds = sessionIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return java.util.Map.of();
        }
        List<TableSessionTable> memberships = membershipRepository.findAllBySessionIdIn(distinctIds);
        if (memberships == null || memberships.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<Long, List<TableSessionTable>> bySession = memberships.stream()
                .filter(m -> m.getSession() != null && m.getSession().getId() != null && m.getTable() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> m.getSession().getId()));

        java.util.Map<Long, String> result = new java.util.HashMap<>();
        bySession.forEach((sessionId, rows) -> {
            List<DiningTable> active = rows.stream()
                    .filter(m -> m.getReleasedAt() == null)
                    .map(TableSessionTable::getTable)
                    .distinct()
                    .sorted(Comparator.comparing(
                            DiningTable::getTableNumber, TableSessionResolver::compareTableNumbers))
                    .toList();
            List<DiningTable> effective = !active.isEmpty()
                    ? active
                    : rows.stream()
                            .map(TableSessionTable::getTable)
                            .distinct()
                            .sorted(Comparator.comparing(
                                    DiningTable::getTableNumber, TableSessionResolver::compareTableNumbers))
                            .toList();
            if (!effective.isEmpty()) {
                result.put(sessionId, displayName(effective));
            }
        });
        return result;
    }

    public boolean tableEverBelongedToSession(@NonNull Long sessionId, @NonNull String tableCode) {
        return membershipRepository.existsAnyBySessionIdAndTableCode(sessionId, tableCode);
    }

    public DiningTable requirePrimaryTable(@NonNull TableSession session) {
        if (session.getTable() != null) {
            return session.getTable();
        }

        return membershipRepository
                .findActivePrimaryBySessionId(session.getId())
                .map(TableSessionTable::getTable)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_SESSION_MEMBERSHIP_NOT_FOUND));
    }

    public boolean tableBelongsToSession(@NonNull Long sessionId, @NonNull String tableCode) {
        return membershipRepository.existsActiveBySessionIdAndTableCode(sessionId, tableCode);
    }

    public String displayName(List<DiningTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return "Table";
        }

        return "Table "
                + tables.stream()
                        .map(DiningTable::getTableNumber)
                        .reduce((left, right) -> left + " + " + right)
                        .orElse("");
    }

    public int totalCapacity(List<DiningTable> tables) {
        if (tables == null) {
            return 0;
        }

        return tables.stream()
                .map(DiningTable::getCapacity)
                .filter(java.util.Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private static int compareTableNumbers(String left, String right) {
        return TableNumberComparator.compare(left, right);
    }
}
