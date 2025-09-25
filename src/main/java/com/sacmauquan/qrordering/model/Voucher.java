package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter @Setter
public class Voucher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private Double discountPercent;
    private Double discountAmount;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private Integer usageLimit;
    private Integer usedCount;
    private Boolean active;
}
