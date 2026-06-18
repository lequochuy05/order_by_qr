package com.qros.modules.order.model;

import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * OrderItemOption - Snapshot entity representing a selected option/topping within an ordered item.
 * Stores values at the time of ordering to maintain historical integrity.
 */
@Entity
@Table(name = "order_item_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE order_item_options SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class OrderItemOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false, length = 50)
    private String optionName;

    @Column(nullable = false, length = 50)
    private String optionValueName;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_option_value_id")
    private ItemOptionValue itemOptionValue;
}
