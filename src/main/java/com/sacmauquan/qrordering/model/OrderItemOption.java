package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "order_item_options")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class OrderItemOption extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    @JsonIgnore
    private OrderItem orderItem;

    @Column(nullable = false)
    private String optionName; // e.g., "Size"

    @Column(nullable = false)
    private String optionValueName; // e.g., "Large"

    private Double extraPrice; // Price at the time of order

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_option_value_id")
    private ItemOptionValue itemOptionValue;
}
