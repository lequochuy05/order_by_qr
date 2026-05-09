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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)

    @JsonIgnoreProperties("payments")
    private Order order;

    @NotNull(message = "Số tiền không được để trống")
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal amount;

    @NotNull(message = "Trạng thái giao dịch không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.PAYOS;

    @Column(length = 500)
    private String checkoutUrl;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @Column(length = 100)
    private String payosReference;

    @Column(length = 255)
    private String cancelReason;

    public enum TransactionStatus {
        PENDING, PAID, CANCELLED, FAILED
    }

    public enum PaymentMethod {
        CASH, PAYOS
    }
}