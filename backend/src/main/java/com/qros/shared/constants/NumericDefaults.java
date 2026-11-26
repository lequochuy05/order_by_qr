package com.qros.shared.constants;

import java.math.BigDecimal;

public final class NumericDefaults {

    private NumericDefaults() {}

    public static BigDecimal safeMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public static long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}
