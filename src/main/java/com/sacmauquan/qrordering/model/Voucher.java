package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE vouchers SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Voucher extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã voucher không được để trống")
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @NotNull(message = "Loại voucher không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type;

    @Column(precision = 15, scale = 2)
    @Min(0)
    private BigDecimal discountAmount; // Số tiền giảm (FIXED_AMOUNT)

    @Min(0)
    private Double discountPercent; // % giảm (PERCENTAGE)

    @Min(1)
    private Integer usageLimit; // Tổng số lần mã này được dùng

    @Builder.Default
    @Column(nullable = false)
    @Min(0)
    private Integer usedCount = 0;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Column(nullable = false)
    private LocalDateTime validFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Column(nullable = false)
    private LocalDateTime validTo;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    public enum VoucherType {
        FIXED_AMOUNT, PERCENTAGE
    }
}