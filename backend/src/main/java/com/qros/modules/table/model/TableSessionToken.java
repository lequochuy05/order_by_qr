package com.qros.modules.table.model;

import com.qros.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "table_session_tokens")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE table_session_tokens SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TableSessionToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @Column(name = "token_hash", length = 128, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime lastSeenAt;

    private LocalDateTime revokedAt;

    public boolean isActive() {
        return revokedAt == null;
    }
}
