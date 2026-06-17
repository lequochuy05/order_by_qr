package com.qros.modules.table.dto.response;

import com.qros.modules.table.model.enums.TableStatus;
import java.time.LocalDateTime;

/**
 * DiningTableResponse - Data transfer object representing a dining table.
 */
public record DiningTableResponse(
        Long id,
        String tableNumber,
        String tableCode,
        TableStatus status,
        Integer capacity,
        String qrCodeUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
