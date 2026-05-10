package com.sacmauquan.qrordering.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderResponse - Data transfer object representing the detailed information of
 * an order.
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
        List<OrderItemResponse> orderItems,
        LocalDateTime createdAt) {
    /**
     * Summary of the dining table associated with the order.
     */
    public record TableSummary(Long id, String tableNumber) {
    }

    /**
     * Response DTO for an individual item within an order.
     */
    public record OrderItemResponse(
            Long id,
            MenuItemSummary menuItem,
            ComboSummary combo,
            BigDecimal unitPrice,
            int quantity,
            String notes,
            boolean prepared,
            String status,
            List<OrderItemOptionResponse> options) {
    }

    public record MenuItemSummary(Long id, String name, CategorySummary category) {
    }

    public record CategorySummary(String name) {
    }

    public record ComboSummary(Long id, String name, BigDecimal price) {
    }

    /**
     * Response DTO for a selected option within an order item.
     */
    public record OrderItemOptionResponse(
            String optionName,
            String optionValueName,
            BigDecimal extraPrice) {
    }
}
