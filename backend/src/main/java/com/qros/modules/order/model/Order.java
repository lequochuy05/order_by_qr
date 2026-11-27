package com.qros.modules.order.model;

import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.user.model.User;
import com.qros.shared.entity.BaseEntity;
import com.qros.shared.enums.PaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Order - Entity representing a customer's order in the system.
 * Stores order lifecycle, payment information, table relation, and order items.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE orders SET is_deleted = true, version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optimistic locking to prevent lost updates when kitchen, cashier,
     * customer, and staff update the same order concurrently.
     */
    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "applied_promotion_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal appliedPromotionPercent = BigDecimal.ZERO;

    @Column(length = 50)
    private String voucherCode;

    @Column(name = "service_charge_percent_snapshot", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal serviceChargePercentSnapshot = BigDecimal.ZERO;

    @Column(name = "service_charge_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal serviceChargeAmount = BigDecimal.ZERO;

    @Column(name = "tax_percent_snapshot", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxPercentSnapshot = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate businessDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private OrderType orderType = OrderType.DINE_IN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    @NotFound(action = NotFoundAction.IGNORE)
    private User paidBy;

    private LocalDateTime paymentTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private DiningTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_session_id")
    private TableSession tableSession;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<OrderItem> orderItems = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<OrderBatch> orderBatches = new LinkedHashSet<>();

    public void addItem(OrderItem item) {
        if (item == null) {
            return;
        }

        orderItems.add(item);
        item.setOrder(this);
    }

    public void addBatch(OrderBatch batch) {
        if (batch == null) {
            return;
        }

        orderBatches.add(batch);
        batch.setOrder(this);
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public boolean isPendingPayment() {
        return paymentStatus == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean isActive() {
        return status == OrderStatus.PENDING || status == OrderStatus.SERVING || status == OrderStatus.AWAITING_PAYMENT;
    }

    public boolean canAcceptNewItems() {
        return !isPaid() && (status == OrderStatus.PENDING || status == OrderStatus.SERVING);
    }

    public boolean canBePaid() {
        return paymentStatus == PaymentStatus.PENDING
                && (status == OrderStatus.AWAITING_PAYMENT
                        || status == OrderStatus.PENDING
                        || status == OrderStatus.SERVING);
    }

    public BigDecimal getEffectiveFinalAmount() {
        return finalAmount != null ? finalAmount : BigDecimal.ZERO;
    }

    public boolean canBeCancelled() {
        return paymentStatus != PaymentStatus.PAID
                && status != OrderStatus.COMPLETED
                && status != OrderStatus.CANCELLED;
    }

    public boolean isDineIn() {
        return orderType == OrderType.DINE_IN;
    }

    public boolean isTakeaway() {
        return orderType == OrderType.TAKEAWAY;
    }
}
