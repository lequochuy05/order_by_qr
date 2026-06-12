package com.qros.modules.order.model;

import com.qros.shared.entity.BaseEntity;
import com.qros.modules.order.model.enums.BatchSource;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_batches")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE order_batches SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class OrderBatch extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private BatchSource source = BatchSource.QR;

    @Column(length = 255)
    private String note;
}
