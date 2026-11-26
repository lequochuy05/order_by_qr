package com.qros.modules.table.service;

import com.qros.modules.table.model.TableSession;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface TableActiveOrderChecker {
    boolean hasActiveOrders(Long tableId);

    default Set<Long> findTableIdsWithActiveOrders(Collection<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> activeTableIds = new HashSet<>();
        for (Long tableId : tableIds) {
            if (tableId != null && hasActiveOrders(tableId)) {
                activeTableIds.add(tableId);
            }
        }
        return activeTableIds;
    }

    default boolean hasUnfinishedItems(Long tableId) {
        return false;
    }

    boolean hasActiveOrdersForSession(Long sessionId);

    boolean hasPaymentInProgressForSession(Long sessionId);

    boolean hasBillableItemsForSession(Long sessionId);

    int countActiveOrdersByTableIds(Collection<Long> tableIds);

    void attachActiveOrdersToSession(Collection<Long> tableIds, TableSession session);

    void cancelEmptyActiveOrdersForSession(Long sessionId);

    default void validateAndPrepareSessionForPaymentRequest(Long sessionId) {}
}
