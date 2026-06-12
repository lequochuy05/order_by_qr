package com.qros.modules.menu.model;

import com.qros.shared.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import jakarta.persistence.*;
import java.util.Set;
import java.util.LinkedHashSet;
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
public class Category extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
