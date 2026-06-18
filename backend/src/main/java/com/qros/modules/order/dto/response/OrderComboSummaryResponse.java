package com.qros.modules.order.dto.response;

import java.math.BigDecimal;

public record OrderComboSummaryResponse(Long id, String name, BigDecimal price) {}
