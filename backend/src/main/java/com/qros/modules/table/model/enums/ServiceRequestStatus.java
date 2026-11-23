package com.qros.modules.table.model.enums;

/**
 * Lifecycle status of a customer table service request.
 */
public enum ServiceRequestStatus {
    PENDING,
    CLAIMED,
    RESOLVED,
    CANCELLED;

    public boolean isOpen() {
        return this == PENDING || this == CLAIMED;
    }
}
