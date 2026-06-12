package com.qros.modules.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.shared.enums.PaymentMethod;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.order.model.enums.PaymentStatus;

/**
 * OrderResponse - Data transfer object representing the detailed information of
 * an order.
 */
public record OrderResponse(
        Long id,
        OrderStatus status,
        String voucherCode,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        LocalDate businessDate,
        OrderType orderType,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        String paidByName,
        LocalDateTime paymentTime,
        OrderTableSummaryResponse table,
        List<OrderItemResponse> orderItems,
        LocalDateTime createdAt) {
}
