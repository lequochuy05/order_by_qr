package com.qros.modules.promotion.model;

import com.qros.modules.order.model.Order;
import com.qros.modules.promotion.model.enums.VoucherType;
import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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

    @Column(name = "code_snapshot", nullable = false, length = 50)
    private String codeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type_snapshot", nullable = false, length = 30)
    private VoucherType discountTypeSnapshot;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentSnapshot;

    @Column(precision = 15, scale = 2)
    private BigDecimal discountAmountSnapshot;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal appliedAmount;

    @Column(nullable = false)
    private LocalDateTime appliedAt;
}
