package com.qros.modules.payment.model;

import com.qros.shared.entity.BaseEntity;
import com.qros.modules.order.model.Order;
import com.qros.modules.user.model.User;
import com.qros.shared.enums.PaymentMethod;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE payment_transactions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PaymentTransaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.PAYOS;

    @Column(length = 500)
    private String checkoutUrl;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @Column(unique = true, length = 100)
    private String externalReference;

    @Column(unique = true, length = 100)
    private String idempotencyKey;

    private LocalDateTime expiresAt;

    private LocalDateTime paidAt;

    private LocalDate businessDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String providerPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(length = 255)
    private String failureReason;

}
