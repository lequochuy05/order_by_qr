package com.qros.modules.table.service;

import com.qros.modules.table.model.TableSession;

public interface TableActiveOrderChecker {
    boolean hasActiveOrders(Long tableId);

    void attachActiveOrderToSession(Long tableId, TableSession session);
}
