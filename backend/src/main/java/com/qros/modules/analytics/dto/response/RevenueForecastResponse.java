package com.qros.modules.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueForecastResponse(
        LocalDate date, BigDecimal actualRevenue, BigDecimal forecastRevenue, boolean forecasted) {}
