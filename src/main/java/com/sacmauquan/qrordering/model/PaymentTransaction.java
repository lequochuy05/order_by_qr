package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * PaymentTransaction - Entity representing an individual payment attempt for an order.
 * Tracks details for external gateway integrations like PayOS.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE payment_transactions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class PaymentTransaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The order associated with this payment transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties("payments")
    private Order order;

    /**
     * Total amount to be processed in this transaction.
     */
    @NotNull(message = "Amount cannot be empty")
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal amount;

    /**
     * Current lifecycle status of the transaction.
     */
    @NotNull(message = "Transaction status cannot be empty")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * Method used for this specific payment attempt.
     */
    @NotNull(message = "Payment method cannot be empty")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.PAYOS;

    /**
     * External URL provided by PayOS for the customer to complete payment.
     */
    @Column(length = 500)
    private String checkoutUrl;

    /**
     * Base64 encoded QR code or raw text for custom QR displays.
     */
    @Column(columnDefinition = "TEXT")
    private String qrCode;

    /**
     * Unique reference identifier returned by the PayOS system.
     */
    @Column(length = 100)
    private String payosReference;

    /**
     * Descriptive reason if the transaction was cancelled or failed.
     */
    @Column(length = 255)
    private String cancelReason;

    /**
     * Enum for payment transaction states.
     */
    public enum TransactionStatus {
        PENDING, PAID, CANCELLED, FAILED
    }

    /**
     * Enum for supported payment methods.
     */
    public enum PaymentMethod {
        CASH, PAYOS
    }
}