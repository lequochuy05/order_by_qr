package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

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

    @NotNull(message = "Trạng thái đơn hàng không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(precision = 15, scale = 2)
    @Min(0)
    private BigDecimal originalTotal;

    @Column(precision = 15, scale = 2)
    @Min(0)
    private BigDecimal discountVoucher;

    @Column(length = 50)
    private String voucherCode;

    @NotNull(message = "Tổng tiền không được để trống")
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal totalAmount;

    @NotNull(message = "Loại đơn hàng không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
    private OrderType orderType = OrderType.DINE_IN;

    @NotNull(message = "Trạng thái thanh toán không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
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

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("order")
    private Set<OrderItem> orderItems = new LinkedHashSet<>();

    public enum OrderStatus {
        PENDING, SERVING, COMPLETED, CANCELLED
    }

    public enum OrderType {
        DINE_IN, TAKEAWAY
    }

    public enum PaymentStatus {
        PENDING, PAID, CANCELLED
    }

    public enum PaymentMethod {
        CASH, PAYOS
    }
}
