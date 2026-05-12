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

/**
 * OrderItemOption - Snapshot entity representing a selected option/topping within an ordered item.
 * Stores values at the time of ordering to maintain historical integrity.
 */
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

    /**
     * The order item this specific option belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    @JsonIgnore
    private OrderItem orderItem;

    /**
     * Captured name of the option group (e.g., "Size").
     */
    @NotBlank(message = "Option name cannot be empty")
    @Column(nullable = false, length = 50)
    private String optionName;

    /**
     * Captured name of the specific value selected (e.g., "Large").
     */
    @NotBlank(message = "Option value name cannot be empty")
    @Column(nullable = false, length = 50)
    private String optionValueName;

    /**
     * Additional price for this option at the time the order was placed.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal extraPrice;

    /**
     * Reference to the original option value entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_option_value_id")
    @JsonIgnore
    private ItemOptionValue itemOptionValue;
}