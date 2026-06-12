package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderDetailProjection {

    Long getOrderId();

    LocalDateTime getPaymentTime();

    String getStaffName();

    BigDecimal getFinalAmount();

    String getTableNumber();
}