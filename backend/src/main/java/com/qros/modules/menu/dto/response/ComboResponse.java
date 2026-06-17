package com.qros.modules.menu.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ComboResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Boolean active,
        Boolean available,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ComboItemResponse> items) {}
