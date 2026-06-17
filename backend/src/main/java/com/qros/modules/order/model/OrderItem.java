package com.qros.modules.order.model;

import com.qros.modules.inventory.model.OrderItemInventoryReservation;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderItemType;
import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private OrderBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(length = 150, nullable = false)
    @Builder.Default
    private String itemNameSnapshot = "";

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private OrderItemType itemType = OrderItemType.MENU_ITEM;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(length = 255)
    private String notes = "";

    @Builder.Default
    private boolean prepared = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
    private OrderItemStatus status = OrderItemStatus.PENDING;

    @Builder.Default
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItemOption> orderItemOptions = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItemInventoryReservation> inventoryReservations = new LinkedHashSet<>();

    public void addOption(OrderItemOption option) {
        if (option == null) {
            return;
        }

        orderItemOptions.add(option);
        option.setOrderItem(this);
    }

    public void addInventoryReservation(OrderItemInventoryReservation reservation) {
        if (reservation == null) {
            return;
        }

        inventoryReservations.add(reservation);
        reservation.setOrderItem(this);
    }

    public boolean isMenuItem() {
        return itemType == OrderItemType.MENU_ITEM;
    }

    public boolean isCombo() {
        return itemType == OrderItemType.COMBO;
    }

    public boolean isPending() {
        return status == OrderItemStatus.PENDING;
    }

    public boolean isCooking() {
        return status == OrderItemStatus.COOKING;
    }

    public boolean isFinished() {
        return status == OrderItemStatus.FINISHED;
    }

    public boolean isCancelled() {
        return status == OrderItemStatus.CANCELLED;
    }

    public boolean isBillable() {
        return status != OrderItemStatus.CANCELLED;
    }

    public boolean canBeMerged() {
        return status == OrderItemStatus.PENDING;
    }

    public boolean canBeUpdated() {
        return status == OrderItemStatus.PENDING;
    }

    public boolean canBeCancelled() {
        return status == OrderItemStatus.PENDING;
    }

    public void markStatus(OrderItemStatus newStatus) {
        this.status = newStatus;
        this.prepared = newStatus == OrderItemStatus.FINISHED;
    }
}
