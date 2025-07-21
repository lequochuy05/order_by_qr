package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
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
    @JoinColumn(name = "cate_id")
    private Category category;
}
