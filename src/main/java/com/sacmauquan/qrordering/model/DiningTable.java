package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tables")
@Getter
@Setter
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_number")  
    private String tableNumber;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    private String status;
    private int capacity;
}
