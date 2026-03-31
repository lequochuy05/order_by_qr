package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "item_option_values")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE item_option_values SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ItemOptionValue extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private double extraPrice = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_option_id")
    private ItemOption itemOption;
}
