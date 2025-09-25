package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "promotions")
@Getter @Setter
public class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double discountPercent;

    private LocalTime startTime;
    private LocalTime endTime;

    private String daysOfWeek; // "MON,TUE,WED"
    private Boolean active;
}
