package com.qros.modules.table.service;

import com.qros.modules.table.model.TableSession;
import java.util.Collection;

public interface TableActiveOrderChecker {
    boolean hasActiveOrders(Long tableId);

    boolean hasActiveOrdersForSession(Long sessionId);

    boolean hasPaymentInProgressForSession(Long sessionId);

    boolean hasBillableItemsForSession(Long sessionId);

    int countActiveOrdersByTableIds(Collection<Long> tableIds);

    void attachActiveOrdersToSession(Collection<Long> tableIds, TableSession session);

    void cancelEmptyActiveOrdersForSession(Long sessionId);
}
