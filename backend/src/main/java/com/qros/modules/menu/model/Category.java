package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.LinkedHashSet;

import java.io.Serializable;

/**
 * Category - Entity representing a grouping for menu items (e.g., Drinks,
 * Appetizers).
 */
@Entity
@Table(name = "category")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE category SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Category extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Unique display name of the category.
     */
    @NotBlank(message = "Category name cannot be empty")
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * URL of the category's representative image or icon.
     */
    @Column(length = 150)
    private String img;

    /**
     * Flag indicating if the category is currently active in the menu.
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * Collection of all menu items belonging to this category.
     */
    @Builder.Default
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("category")
    private Set<MenuItem> menuItems = new LinkedHashSet<>();
}
