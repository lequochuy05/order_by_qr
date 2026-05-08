package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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

    @NotBlank(message = "Mã bàn không được để trống")
    @Column(length = 5, nullable = false, unique = true)
    private String tableNumber;

    @NotBlank(message = "Mã QR không được để trống")
    @Column(length = 50, nullable = false, unique = true)
    private String tableCode;

    @NotBlank(message = "Link QR không được để trống")
    @Column(length = 150, nullable = false, unique = true)
    private String qrCodeUrl;

    @Column(length = 50, nullable = false, unique = true)
    private String qrCodePublicId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(nullable = false)
    @Min(1)
    private Integer capacity;

    public enum TableStatus {
        AVAILABLE,
        OCCUPIED,
        WAITING_FOR_PAYMENT
    }
}
