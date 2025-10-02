package com.sacmauquan.qrordering.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "combos")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter @Setter
public class Combo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false)
    private Double price;

    private Boolean active;

    @OneToMany(mappedBy = "combo", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"combo"})
    private List<ComboItem> items;
}
