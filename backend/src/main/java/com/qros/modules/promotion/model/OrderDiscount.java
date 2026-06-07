package com.qros.modules.promotion.model;

import com.qros.modules.order.model.Order;
import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_discounts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE order_discounts SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class OrderDiscount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(length = 50)
    private String codeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Voucher.VoucherType discountTypeSnapshot;

    private Double discountPercentSnapshot;

    @Column(precision = 15, scale = 2)
    @Min(0)
    private BigDecimal discountAmountSnapshot;

    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal appliedAmount;

    private LocalDateTime appliedAt;
}
