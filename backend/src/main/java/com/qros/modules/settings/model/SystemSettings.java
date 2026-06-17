package com.qros.modules.settings.model;

import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE system_settings SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class SystemSettings extends BaseEntity {

    @Id
    private Long id;

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
}
