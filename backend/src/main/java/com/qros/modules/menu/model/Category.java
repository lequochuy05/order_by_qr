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
@SQLDelete(sql = "UPDATE category SET is_deleted = true, version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String img;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "category")
    private Set<MenuItem> menuItems = new LinkedHashSet<>();
}
