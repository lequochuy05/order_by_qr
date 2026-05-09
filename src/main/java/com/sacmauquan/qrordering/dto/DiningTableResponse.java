package com.sacmauquan.qrordering.dto;

import java.time.LocalDateTime;

/**
 * DiningTableResponse - Dữ liệu trả về cho danh sách bàn ăn.
 * Sử dụng Record để đảm bảo tính bất biến (Immutable).
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
