package com.qros.modules.order.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ActiveOrderSummary {

    Long getId();

    String getStatus();

    String getPaymentStatus();

    BigDecimal getFinalAmount();

    Long getTableId();

    String getTableNumber();

    Long getSessionId();

    LocalDateTime getCreatedAt();
}
