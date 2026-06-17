package com.qros.modules.table.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * DiningTable - Entity representing a physical table in the restaurant.
 * Linked to a unique QR code for customer ordering.
 */
@Entity
@Table(name = "tables")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE tables SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiningTable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)
    private String tableNumber;

    @Column(length = 50, nullable = false)
    private String tableCode;

    @Column(length = 500, nullable = false)
    private String qrCodeUrl;

    @Column(length = 50, nullable = false)
    private String qrCodePublicId;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(nullable = false)
    private Integer capacity;
}
