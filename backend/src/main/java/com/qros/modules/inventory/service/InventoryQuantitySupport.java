package com.qros.modules.inventory.service;

import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class InventoryQuantitySupport {

    public static final int QUANTITY_SCALE = 3;

    private InventoryQuantitySupport() {}

    public static BigDecimal normalizePositiveQuantity(BigDecimal value) {
        BigDecimal normalized = normalizeQuantity(value);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }
        return normalized;
    }

    public static BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal quantityFrom(Number value) {
        if (value == null) {
            return BigDecimal.ONE.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal decimalValue) {
            return normalizeQuantity(decimalValue);
        }
        if (value instanceof Integer intValue) {
            return BigDecimal.valueOf(intValue).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }
}
