package com.sacmauquan.qrordering.model;

import java.util.List;

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
import java.util.ArrayList;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    private String name;
    
    private String img;

    private double price;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "cate_id", nullable = false)
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"menuItem"})
    private Set<ItemOption> itemOptions = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ComboItem> comboItems = new LinkedHashSet<>();


}
