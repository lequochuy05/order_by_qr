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
