package com.qros.modules.order.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ActiveOrderSummary {

    Long getId();

    String getStatus();

    BigDecimal getFinalAmount();

    Long getTableId();

    String getTableNumber();

    LocalDateTime getCreatedAt();
}