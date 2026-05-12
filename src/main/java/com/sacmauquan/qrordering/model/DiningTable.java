package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class DiningTable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display label for the table (e.g., "A1", "05").
     */
    @NotBlank(message = "Table number cannot be empty")
    @Column(length = 10, nullable = false, unique = true)
    private String tableNumber;

    /**
     * Unique identifier encoded in the table's QR code.
     */
    @NotBlank(message = "Table code cannot be empty")
    @Column(length = 50, nullable = false, unique = true)
    private String tableCode;

    /**
     * URL of the QR code image stored on Cloudinary.
     */
    @NotBlank(message = "QR Code URL cannot be empty")
    @Column(length = 150, nullable = false, unique = true)
    private String qrCodeUrl;

    /**
     * Cloudinary's public ID for the QR code image.
     */
    @Column(length = 50, nullable = false, unique = true)
    private String qrCodePublicId;

    /**
     * Current availability status of the table.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TableStatus status = TableStatus.AVAILABLE;

    /**
     * Maximum number of people the table can accommodate.
     */
    @Column(nullable = false)
    @Min(1)
    private Integer capacity;

    /**
     * Enum for dining table lifecycle states.
     */
    public enum TableStatus {
        AVAILABLE,
        OCCUPIED,
        WAITING_FOR_PAYMENT
    }
}
