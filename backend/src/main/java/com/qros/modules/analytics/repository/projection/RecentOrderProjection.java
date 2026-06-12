package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RecentOrderProjection {

    Long getOrderId();

    String getStatus();

    BigDecimal getFinalAmount();

    LocalDateTime getCreatedAt();

    LocalDateTime getPaymentTime();

    String getTableNumber();
}