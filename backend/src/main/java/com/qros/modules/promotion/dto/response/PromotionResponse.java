package com.qros.modules.promotion.dto.response;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record PromotionResponse(
        Long id,
        String name,
        BigDecimal discountPercent,
        LocalTime startTime,
        LocalTime endTime,
        Set<DayOfWeek> daysOfWeek,
        Boolean active) {
}