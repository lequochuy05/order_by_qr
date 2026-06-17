package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * ItemOptionValue - Entity representing a specific choice within an ItemOption
 * (e.g., "Regular Size", "Large Size").
 */
@Entity
@Table(name = "item_option_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE item_option_values SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ItemOptionValue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_option_id", nullable = false)
    private ItemOption itemOption;
}
