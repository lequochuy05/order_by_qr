package com.qros.modules.analytics.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderDetailProjection {

    Long getOrderId();

    LocalDateTime getPaymentTime();

    String getUserName();

    BigDecimal getFinalAmount();

    String getTableNumber();
}
