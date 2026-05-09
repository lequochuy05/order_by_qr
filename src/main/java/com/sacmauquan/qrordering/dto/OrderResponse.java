package com.sacmauquan.qrordering.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderResponse - Dữ liệu trả về cho đơn hàng.
 */
public record OrderResponse(
    Long id,
    String status,
    BigDecimal originalTotal,
    BigDecimal discountVoucher,
    String voucherCode,
    BigDecimal totalAmount,
    String orderType,
    String paymentStatus,
    String paymentMethod,
    String paidByName,
    LocalDateTime paymentTime,
    TableSummary table,
    List<OrderItemResponse> items,
    LocalDateTime createdAt
) {
    public record TableSummary(Long id, String tableNumber) {}

    public record OrderItemResponse(
        Long id,
        String name,
        BigDecimal unitPrice,
        int quantity,
        String notes,
        boolean prepared,
        String status,
        List<OrderItemOptionResponse> options
    ) {}

    public record OrderItemOptionResponse(
        String optionName,
        String optionValueName,
        BigDecimal extraPrice
    ) {}
}
