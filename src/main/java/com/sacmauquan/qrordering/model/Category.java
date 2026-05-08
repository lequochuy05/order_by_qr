package com.sacmauquan.qrordering.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.LinkedHashSet;

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

    @NotBlank(message = "Tên danh mục không được để trống")
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @NotBlank(message = "Ảnh danh mục không được để trống")
    @Column(length = 150, nullable = false)
    private String img;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("category")
    private Set<MenuItem> menuItems = new LinkedHashSet<>();
}
