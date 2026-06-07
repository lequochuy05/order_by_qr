package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ComboItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Combo is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    @JsonIgnoreProperties("items")
    private Combo combo;

    @NotNull(message = "Menu item is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)

    @JsonIgnoreProperties({ "category", "comboItems" })
    private MenuItem menuItem;

    @Builder.Default
    @Column(nullable = false)
    @Min(1)
    private Integer quantity = 1;
}
