package com.qros.modules.settings.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "system_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {

    @Id
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, length = 150)
    private String restaurantName;

    @Column(length = 30)
    private String restaurantPhone;

    @Column(length = 150)
    private String restaurantEmail;

    @Column(length = 255)
    private String restaurantAddress;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 100)
    private String wifiName;

    @Column(length = 255)
    private String wifiPassword;

    private LocalTime openingTime;

    private LocalTime closingTime;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal serviceChargePercent;

    @Column(nullable = false)
    private Boolean orderingEnabled;

    @Column(nullable = false)
    private Boolean maintenanceMode;

    @Column(nullable = false)
    private Boolean cashPaymentEnabled;

    @Column(nullable = false)
    private Boolean onlinePaymentEnabled;

    @Column(nullable = false)
    private Integer paymentQrExpiresInMinutes;

    @Column(nullable = false)
    private Boolean autoConfirmOrders;

    @Column(nullable = false)
    private Integer kitchenOverdueThresholdMinutes;

    @Column(nullable = false)
    private Boolean showUnavailableItems;

    @Column(nullable = false)
    private Boolean showRecommendations;

    @Column(nullable = false)
    private Boolean showCombos;

    @Column(nullable = false, length = 100)
    private String billTitle;

    @Column(nullable = false, length = 255)
    private String billFooterMessage;

    @Column(nullable = false, length = 10)
    private String billPaperSize;

    @Column(nullable = false)
    private Boolean showWifiOnBill;

    @Column(nullable = false)
    private Boolean autoPrintBill;

    @Column(nullable = false)
    private Boolean newOrderNotificationEnabled;

    @Column(nullable = false)
    private Boolean paymentNotificationEnabled;

    @Column(nullable = false)
    private Boolean kitchenOverdueNotificationEnabled;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
