package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double discountPercent;

    private LocalTime startTime;
    private LocalTime endTime;

    private String daysOfWeek; // "MON,TUE,WED"
    @Builder.Default
    private Boolean active = true;
}
