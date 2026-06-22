package com.qros.shared.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * BaseEntity - Abstract base class for all JPA entities.
 * Provides automated auditing fields for creation and update timestamps,
 * along with soft-delete support via {@code is_deleted} flag.
 * <p>
 * All entities inheriting from this class will automatically exclude
 * soft-deleted rows from queries unless explicitly bypassed.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("is_deleted = false")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isDeleted = false;
}
