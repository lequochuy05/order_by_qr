package com.sacmauquan.qrordering.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * BaseEntity - Abstract base class for all JPA entities.
 * Provides automated auditing fields for creation and update timestamps.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class BaseEntity {

    /**
     * Timestamp when the record was first created.
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last modified.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Soft delete flag to indicate if the record is logically removed.
     */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isDeleted = false;
}
