package com.qros.modules.table.repository;

import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.enums.ServiceRequestReason;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableServiceRequestRepository extends JpaRepository<TableServiceRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM TableServiceRequest r
            JOIN FETCH r.table
            JOIN FETCH r.tableSession
            WHERE r.id = :id
            """)
    Optional<TableServiceRequest> findByIdForUpdate(@Param("id") Long id);

    Optional<TableServiceRequest> findFirstByTableSessionIdAndClientRequestId(
            Long tableSessionId, String clientRequestId);

    Optional<TableServiceRequest> findTopByTableIdAndTableSessionIdOrderByCreatedAtDesc(
            Long tableId, Long tableSessionId);

    long countByTableSessionIdAndStatusIn(Long tableSessionId, Collection<ServiceRequestStatus> statuses);

    boolean existsByTableIdAndReasonAndStatusIn(
            Long tableId, ServiceRequestReason reason, Collection<ServiceRequestStatus> statuses);

    @Query("""
            SELECT r
            FROM TableServiceRequest r
            JOIN FETCH r.table
            JOIN FETCH r.tableSession
            LEFT JOIN FETCH r.claimedByUser
            LEFT JOIN FETCH r.resolvedByUser
            WHERE r.tableSession.id = :sessionId
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<TableServiceRequest> findBySessionIdOrderByCreatedAtDesc(@Param("sessionId") Long sessionId);

    @Query("""
            SELECT r
            FROM TableServiceRequest r
            JOIN FETCH r.table
            JOIN FETCH r.tableSession
            LEFT JOIN FETCH r.claimedByUser
            LEFT JOIN FETCH r.resolvedByUser
            WHERE r.status IN :statuses
              AND (CAST(:tableId AS long) IS NULL OR r.table.id = :tableId)
              AND (CAST(:claimedByUserId AS long) IS NULL OR r.claimedByUser.id = :claimedByUserId)
            ORDER BY r.createdAt ASC, r.id ASC
            """)
    List<TableServiceRequest> findByStatusesOrderByCreatedAtAsc(
            @Param("statuses") Collection<ServiceRequestStatus> statuses,
            @Param("tableId") Long tableId,
            @Param("claimedByUserId") Long claimedByUserId);

    @Query("""
            SELECT r
            FROM TableServiceRequest r
            JOIN FETCH r.table
            JOIN FETCH r.tableSession
            LEFT JOIN FETCH r.claimedByUser
            LEFT JOIN FETCH r.resolvedByUser
            WHERE r.status IN :statuses
              AND (CAST(:tableId AS long) IS NULL OR r.table.id = :tableId)
              AND (CAST(:claimedByUserId AS long) IS NULL OR r.claimedByUser.id = :claimedByUserId)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<TableServiceRequest> findByStatusesOrderByCreatedAtDesc(
            @Param("statuses") Collection<ServiceRequestStatus> statuses,
            @Param("tableId") Long tableId,
            @Param("claimedByUserId") Long claimedByUserId);

    @Query("""
            SELECT r
            FROM TableServiceRequest r
            JOIN FETCH r.table
            WHERE r.createdAt >= :from
              AND r.createdAt < :toExclusive
            ORDER BY r.createdAt DESC
            """)
    List<TableServiceRequest> findByCreatedAtRange(
            @Param("from") LocalDateTime from, @Param("toExclusive") LocalDateTime toExclusive);

    @Modifying
    @Query("""
            UPDATE TableServiceRequest r
            SET r.status = :cancelledStatus,
                r.cancelledByType = :cancelledByType,
                r.cancelledReason = :cancelledReason,
                r.updatedAt = :now,
                r.version = r.version + 1
            WHERE r.tableSession.id = :sessionId
              AND r.status IN :openStatuses
              AND r.isDeleted = false
            """)
    int closeOpenRequestsBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("cancelledStatus") ServiceRequestStatus cancelledStatus,
            @Param("openStatuses") Collection<ServiceRequestStatus> openStatuses,
            @Param("cancelledByType") String cancelledByType,
            @Param("cancelledReason") String cancelledReason,
            @Param("now") LocalDateTime now);
}
