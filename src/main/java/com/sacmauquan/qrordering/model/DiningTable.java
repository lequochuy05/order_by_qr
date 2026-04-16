package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tables")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class DiningTable extends BaseEntity {
    public static final String AVAILABLE = "AVAILABLE";
    public static final String OCCUPIED = "OCCUPIED";
    public static final String WAITING_FOR_PAYMENT = "WAITING_FOR_PAYMENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_number", nullable = false, unique = true, length = 20)
    private String tableNumber;

    @Column(name = "qr_code_url", length = 255)
    private String qrCodeUrl;

    @Column(name = "qr_code_public_id", length = 255)
    private String qrCodePublicId;

    @Column(name = "table_code", nullable = false, unique = true, length = 50)
    private String tableCode;

    @Builder.Default
    private String status = AVAILABLE;
    private int capacity;
}
