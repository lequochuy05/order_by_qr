package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "orders")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private DiningTable table;

    @Column(name = "status")
    private String status;

    @Column(name = "original_total")
    private Double originalTotal;

    @Column(name = "discount_voucher")
    private Double discountVoucher;

    @Column(name = "voucher_code")
    private String voucherCode;

    @Column(name = "total_amount")
    private double totalAmount;

    @Builder.Default
    @Column(name = "order_type")
    private String orderType = "DINE_IN"; // DINE_IN, TAKEAWAY

    @Builder.Default
    @Column(name = "payment_status")
    private String paymentStatus = "PENDING"; // PENDING, COMPLETED, CANCELLED

    @Column(name = "payment_method")
    private String paymentMethod; // CASH, TRANSFER, VNPAY

    @ManyToOne
    @JoinColumn(name = "paid_by")
    private User paidBy;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> orderItems = new ArrayList<>();

}
