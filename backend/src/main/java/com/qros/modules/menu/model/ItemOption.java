package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * ItemOption - Entity representing a customizable choice for a menu item (e.g.,
 * Size, Toppings).
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "item_option")
@SQLDelete(sql = "UPDATE item_option SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ItemOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean required = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer maxSelection = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Builder.Default
    @OneToMany(mappedBy = "itemOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ItemOptionValue> optionValues = new LinkedHashSet<>();
}
