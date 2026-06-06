package com.qros.modules.promotion.model;

import com.qros.shared.entity.BaseEntity;
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

/**
 * Voucher - Entity representing a discount code that can be applied to orders.
 * Supports both fixed amount and percentage-based discounts.
 */
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

    /**
     * Unique code string used by customers to claim the discount.
     */
    @NotBlank(message = "Voucher code cannot be empty")
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Type of discount calculation (e.g., FIXED_AMOUNT, PERCENTAGE).
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'FIXED_AMOUNT'")
    private VoucherType type = VoucherType.FIXED_AMOUNT;

    /**
     * Absolute monetary value to deduct if type is FIXED_AMOUNT.
     */
    @Column(precision = 15, scale = 2)
    @Min(value = 0, message = "Discount amount cannot be negative")
    private BigDecimal discountAmount;

    /**
     * Percentage value (0-100) to deduct if type is PERCENTAGE.
     */
    @Min(value = 0, message = "Discount percentage cannot be negative")
    private Double discountPercent;

    /**
     * Maximum number of times this voucher can be redeemed in total.
     */
    @Min(value = 0, message = "Usage limit cannot be negative")
    private Integer usageLimit;

    /**
     * Current count of how many times this voucher has been successfully used.
     */
    @Builder.Default
    @Column(nullable = false)
    @Min(value = 0, message = "Used count cannot be negative")
    private Integer usedCount = 0;

    /**
     * Start time when the voucher becomes active.
     */
    @NotNull(message = "Valid from date cannot be empty")
    @Column(nullable = false)
    private LocalDateTime validFrom;

    /**
     * End time after which the voucher expires.
     */
    @NotNull(message = "Valid to date cannot be empty")
    @Column(nullable = false)
    private LocalDateTime validTo;

    /**
     * Flag indicating if the voucher is currently enabled for use.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Enum for voucher discount calculation methods.
     */
    public enum VoucherType {
        FIXED_AMOUNT, PERCENTAGE
    }
}