package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * MenuItem - Entity representing a single dish or beverage on the menu.
 * Includes details such as price, category, and customizable options.
 */
@Entity
@Table(name = "menu_item")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE menu_item SET is_deleted = true, version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class MenuItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 500)
    private String img;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean available = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cate_id", nullable = false)
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ItemOption> itemOptions = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "menuItem", fetch = FetchType.LAZY)
    private Set<ComboItem> comboItems = new LinkedHashSet<>();
}
