package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Combo - Entity representing a promotional package containing multiple menu
 * items at a fixed price.
 */
@Entity
@Table(name = "combos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE combos SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Combo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)    
    private Boolean available = true;

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ComboItem> items = new LinkedHashSet<>();
}
