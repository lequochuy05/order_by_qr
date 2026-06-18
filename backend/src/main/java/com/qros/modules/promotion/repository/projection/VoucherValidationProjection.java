package com.qros.modules.promotion.repository.projection;

import com.qros.modules.promotion.model.enums.VoucherType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VoucherValidationProjection {

    Long getId();

    String getCode();

    VoucherType getType();

    BigDecimal getDiscountAmount();

    BigDecimal getDiscountPercent();

    Integer getUsageLimit();

    Integer getUsedCount();

    LocalDateTime getValidFrom();

    LocalDateTime getValidTo();

    Boolean getActive();
}
