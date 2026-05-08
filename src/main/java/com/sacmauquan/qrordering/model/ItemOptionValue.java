package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "item_option_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE item_option_values SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ItemOptionValue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên giá trị lựa chọn không được để trống")
    @Column(length = 50, nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @NotNull(message = "Lựa chọn không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_option_id", nullable = false)
    @JsonIgnoreProperties({ "optionValues", "menuItem" })
    private ItemOption itemOption;
}
