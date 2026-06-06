package com.qros.modules.payment.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PayosCreateResponse - Data transfer object for the response after creating a
 * PayOS transaction.
 */
@Data
@Builder
public class PayosCreateResponse {
    /**
     * The internal transaction ID.
     */
    private Long transactionId;

    /**
     * The URL where the customer can complete the payment.
     */
    private String checkoutUrl;
    private String qrCode;
    private LocalDateTime createdAt;
    private BigDecimal amount;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal originalTotal;
    private BigDecimal discountVoucher;
    private String voucherCode;
    private String idempotencyKey;
    private String externalReference;
}
