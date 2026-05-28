package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE promotions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
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
