package com.qros.modules.order.repository.projection;

public interface ActiveOrderLockProjection {
    Long getId();

    Long getTableSessionId();
}
