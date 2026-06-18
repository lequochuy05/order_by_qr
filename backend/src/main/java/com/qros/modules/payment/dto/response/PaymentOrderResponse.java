package com.qros.modules.payment.dto.response;

import com.qros.shared.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentOrderResponse(
        Long id,
        String status,
        String voucherCode,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        LocalDate businessDate,
        String orderType,
        String paymentStatus,
        PaymentMethod paymentMethod,
        String paidByName,
        LocalDateTime paymentTime,
        TableSummary table,
        List<Item> orderItems,
        LocalDateTime createdAt) {

    public record TableSummary(Long id, String tableNumber) {}

    public record Item(
            Long id,
            Long batchId,
            MenuItemSummary menuItem,
            ComboSummary combo,
            String itemNameSnapshot,
            String itemType,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal,
            String notes,
            boolean prepared,
            String status,
            List<ItemOption> options,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    public record MenuItemSummary(Long id, String name, CategorySummary category) {}

    public record CategorySummary(String name) {}

    public record ComboSummary(Long id, String name, BigDecimal price) {}

    public record ItemOption(Long valueId, String optionName, String optionValueName, BigDecimal extraPrice) {}
}
