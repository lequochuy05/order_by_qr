package com.qros.modules.order.model;

import com.qros.shared.entity.BaseEntity;
import com.qros.modules.user.model.User;
import com.qros.modules.table.model.DiningTable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.BatchSize;

/**
 * Order - Entity representing a customer's order in the system.
 * Manages order status, payments, and associated items.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE orders SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Current workflow status of the order.
     */
    @NotNull(message = "Order status cannot be empty")
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Subtotal before discounts.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal subtotalAmount;

    /**
     * Total discount applied to this order.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal discountAmount;

    /**
     * The voucher code applied to this order.
     */
    @Column(length = 50)
    private String voucherCode;

    /**
     * Final amount due after discounts.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal finalAmount;

    /**
     * Amount already paid against this order.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal paidAmount;

    /**
     * Business date used for restaurant reporting.
     */
    @Column(nullable = false)
    private LocalDate businessDate;

    /**
     * Type of order (DINE_IN or TAKEAWAY).
     */
    @NotNull(message = "Order type cannot be empty")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
    private OrderType orderType = OrderType.DINE_IN;

    /**
     * Current payment lifecycle status.
     */
    @NotNull(message = "Payment status cannot be empty")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Method used for payment (e.g., CASH, PAYOS).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    /**
     * The user (staff/manager) who processed the payment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;

    /**
     * Precise time when the payment was confirmed.
     */
    private LocalDateTime paymentTime;

    /**
     * The dining table associated with this order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private DiningTable table;

    /**
     * Collection of individual items included in the order.
     */
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("order")
    @BatchSize(size = 20)
    private Set<OrderItem> orderItems = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("order")
    private Set<OrderBatch> orderBatches = new LinkedHashSet<>();

    /**
     * Enum for order workflow states.
     */
    public enum OrderStatus {
        PENDING, SERVING, AWAITING_PAYMENT, COMPLETED, CANCELLED
    }

    /**
     * Enum for order consumption types.
     */
    public enum OrderType {
        DINE_IN, TAKEAWAY
    }

    /**
     * Enum for payment lifecycle states.
     */
    public enum PaymentStatus {
        PENDING, PAID, CANCELLED
    }

    /**
     * Enum for supported payment methods.
     */
    public enum PaymentMethod {
        CASH, PAYOS
    }
}
