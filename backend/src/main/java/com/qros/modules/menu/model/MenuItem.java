package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * MenuItem - Entity representing a single dish or beverage on the menu.
 * Includes details such as price, category, and customizable options.
 */
@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Table(name = "menu_item")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE menu_item SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class MenuItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display name of the menu item.
     */
    @NotBlank(message = "Item name cannot be empty")
    @Column(length = 50, nullable = false)
    private String name;

    /**
     * URL of the item's representative image.
     */
    @NotBlank(message = "Item image cannot be empty")
    @Column(length = 150)
    @Builder.Default
    private String img = "default_menu_item.png";

    /**
     * Base price of the item.
     */
    @NotNull(message = "Item price cannot be empty")
    @Column(nullable = false)
    @Min(value = 0, message = "Item price cannot be negative")
    private BigDecimal price;

    /**
     * Flag indicating if the item is currently available for ordering.
     */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean active = true;

    /**
     * The category this item belongs to.
     */
    @NotNull(message = "Category cannot be empty")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cate_id", nullable = false)
    @JsonIgnoreProperties("menuItems")
    private Category category;

    /**
     * Collection of customizable options (e.g., Sugar Level, Toppings) for this
     * item.
     */
    @Builder.Default
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "menuItem" })
    private Set<ItemOption> itemOptions = new LinkedHashSet<>();

    /**
     * Collection of combo packages that include this item.
     */
    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ComboItem> comboItems = new LinkedHashSet<>();

}
