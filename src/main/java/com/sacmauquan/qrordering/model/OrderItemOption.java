package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "order_item_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE order_item_options SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class OrderItemOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    @JsonIgnore
    private OrderItem orderItem;

    @NotBlank(message = "Tên lựa chọn không được để trống")
    @Column(nullable = false, length = 50)
    private String optionName; // Size

    @NotBlank(message = "Giá trị lựa chọn không được để trống")
    @Column(nullable = false, length = 50)
    private String optionValueName; // "M", "L"

    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal extraPrice; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_option_value_id")
    @JsonIgnore
    private ItemOptionValue itemOptionValue;
}