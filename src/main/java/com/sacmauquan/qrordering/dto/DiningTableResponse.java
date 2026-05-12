package com.sacmauquan.qrordering.dto;

import java.time.LocalDateTime;

/**
 * DiningTableResponse - Data transfer object representing a dining table.
 */
public record DiningTableResponse(
    Long id,
    String tableNumber,
    String tableCode,
    String status,
    int capacity,
    String qrCodeUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
