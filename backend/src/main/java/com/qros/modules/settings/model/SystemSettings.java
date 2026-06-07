package com.qros.modules.settings.model;

import com.qros.shared.util.AppTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SystemSettings - Single-row restaurant and operation configuration.
 */
@Entity
@Table(name = "system_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Restaurant name cannot be empty")
    @Column(nullable = false, length = 100)
    private String restaurantName;

    @Column(columnDefinition = "TEXT")
    private String restaurantAddress;

    @Column(length = 20)
    private String restaurantPhone;

    @Column(columnDefinition = "TEXT")
    private String restaurantLogoUrl;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "VAT rate cannot be negative")
    @Column(precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(length = 50)
    private String wifiSsid;

    @Column(length = 50)
    private String wifiPassword;

    @Builder.Default
    @Column(nullable = false)
    private Boolean autoApproveOrders = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enableAiAssistant = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enablePayos = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enableCash = true;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = AppTime.now();
    }
}
