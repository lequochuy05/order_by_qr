package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.Set;
import java.util.LinkedHashSet;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

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

    @NotBlank(message = "Tên món ăn không được để trống")
    @Column(length = 50, nullable = false)
    private String name;

    @NotBlank(message = "Ảnh món ăn không được để trống")
    @Column(length = 150)
    private String img;

    @NotNull(message = "Giá món ăn không được để trống")
    @Column(nullable = false)
    @Min(value = 0, message = "Giá món ăn không được âm")
    private BigDecimal price;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean active = true;

    @NotNull(message = "Danh mục không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cate_id", nullable = false)
    @JsonIgnoreProperties("menuItems")
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "menuItem" })
    private Set<ItemOption> itemOptions = new LinkedHashSet<>();

    @Builder.Default
    @JsonIgnoreProperties("menuItem")
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ComboItem> comboItems = new LinkedHashSet<>();

}
