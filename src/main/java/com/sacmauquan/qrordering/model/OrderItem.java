package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_item")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @ManyToOne
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @Column(name = "unit_price")
    private double unitPrice;

    @Column(name = "quantity")
    private int quantity;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "prepared")
    private boolean prepared = false;

}
