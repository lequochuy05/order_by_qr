package com.sacmauquan.qrordering.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "menu_item")
@Getter
@Setter
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private double price;

    private String img;

    @ManyToOne
    @JoinColumn(name = "cate_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "menuItem", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"menuItem"})   // tránh vòng lặp khi serialize
    private List<ComboItem> comboItems;


}
