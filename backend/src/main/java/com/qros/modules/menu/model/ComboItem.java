package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
/**
 * ComboItem - Represents a specific menu item bundled within a combo meal.
 */
@Entity
@Table(name = "combo_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE combo_items SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ComboItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)

    private MenuItem menuItem;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;
}
