package com.qros.modules.ai.dto.response;

import java.math.BigDecimal;

public record AiSuggestedItem(
        String type,
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        String categoryName,
        int quantity,
        String reason,
        boolean hasOptions,
        boolean available) {}
