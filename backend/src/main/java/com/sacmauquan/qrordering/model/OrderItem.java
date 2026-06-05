package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.experimental.SuperBuilder;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * OrderItem - Entity representing an individual line item within an order.
 * Tracks quantity, specific choices (options), and preparation status.
 */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE order_item SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent order this item belongs to.
     */
    @NotNull(message = "Order cannot be empty")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties("orderItems")
    private Order order;

    /**
     * The specific menu item being ordered (if applicable).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    @JsonIgnoreProperties({ "itemOptions", "comboItems" })
    private MenuItem menuItem;

    /**
     * The specific combo package being ordered (if applicable).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    @JsonIgnoreProperties({ "items" })
    private Combo combo;

    /**
     * Price of the item at the time of ordering.
     */
    @NotNull(message = "Unit price cannot be empty")
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal unitPrice;

    /**
     * Number of units ordered.
     */
    @NotNull(message = "Quantity cannot be empty")
    @Column(nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    /**
     * Customer's special instructions for this item.
     */
    @Builder.Default
    private String notes = "";

    /**
     * Flag indicating if the kitchen has finished preparing this item.
     */
    @Builder.Default
    private boolean prepared = false;

    /**
     * Current workflow status of the individual item.
     */
    @NotNull(message = "Item status cannot be empty")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
    private OrderItemStatus status = OrderItemStatus.PENDING;

    /**
     * Collection of selected options and toppings for this order item.
     */
    @Builder.Default
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "orderItem" })
    private Set<OrderItemOption> orderItemOptions = new LinkedHashSet<>();

    /**
     * Enum for individual order item states.
     */
    public enum OrderItemStatus {
        PENDING, COOKING, FINISHED, CANCELLED
    }
}
