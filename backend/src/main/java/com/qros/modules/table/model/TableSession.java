package com.qros.modules.table.model;

import com.qros.modules.table.model.enums.TableSessionSource;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "table_sessions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE table_sessions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TableSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private TableSessionStatus status = TableSessionStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    @Column(nullable = false)
    private LocalDateTime lastActivityAt;

    private LocalDateTime closedAt;

    @Column(length = 255)
    private String closedReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private TableSessionSource createdSource = TableSessionSource.CUSTOMER;

    public boolean isOpen() {
        return status == TableSessionStatus.OPEN;
    }
}
