package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    private Double discountAmount;
    private Double discountPercent;

    private Integer usageLimit;

    @Builder.Default
    @Column(nullable = false)
    private Integer usedCount = 0;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
