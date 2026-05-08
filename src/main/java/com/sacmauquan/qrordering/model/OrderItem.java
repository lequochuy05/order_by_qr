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

    @NotNull(message = "Đơn hàng không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties("orderItems")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    @JsonIgnoreProperties({ "itemOptions", "comboItems" })
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    @JsonIgnoreProperties({ "items" })
    private Combo combo;

    @NotNull(message = "Giá đơn vị không được để trống")
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal unitPrice;

    @NotNull(message = "Số lượng không được để trống")
    @Column(nullable = false)
    @Min(1)
    private int quantity;

    @Builder.Default
    private String notes = "";

    @Builder.Default
    private boolean prepared = false;

    @NotNull(message = "Trạng thái món không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
    private OrderItemStatus status = OrderItemStatus.PENDING;

    @Builder.Default
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "orderItem" })
    private Set<OrderItemOption> orderItemOptions = new LinkedHashSet<>();

    public enum OrderItemStatus {
        PENDING, COOKING, READY, SERVED, CANCELLED
    }
}
