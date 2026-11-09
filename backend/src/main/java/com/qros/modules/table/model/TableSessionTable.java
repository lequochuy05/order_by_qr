package com.qros.modules.table.model;

import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "table_session_tables")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(
        sql = "UPDATE table_session_tables SET is_deleted = true, version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class TableSessionTable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primaryTable = false;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime releasedAt;

    private Long joinedByUserId;

    public boolean isActive() {
        return releasedAt == null;
    }
}
