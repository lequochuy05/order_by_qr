package com.qros.modules.table.repository;

import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.TableSessionStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "table")
    @Query(
            """
            SELECT s
            FROM TableSession s
            WHERE s.table.id = :tableId
              AND s.status = :status
            """)
    Optional<TableSession> findByTableIdAndStatusForUpdate(
            @Param("tableId") Long tableId, @Param("status") TableSessionStatus status);

    @EntityGraph(attributePaths = "table")
    Optional<TableSession> findFirstByTableIdAndStatusOrderByOpenedAtDesc(Long tableId, TableSessionStatus status);

    boolean existsByTableIdAndStatus(Long tableId, TableSessionStatus status);

    @EntityGraph(attributePaths = "table")
    List<TableSession> findByStatus(TableSessionStatus status);

    @Query("SELECT s.table.id FROM TableSession s WHERE s.id = :id")
    Optional<Long> findTableIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "table")
    @Query("SELECT s FROM TableSession s WHERE s.id = :id")
    Optional<TableSession> findByIdForUpdate(@Param("id") Long id);

    default int touchOpenSessionActivity(Long id, LocalDateTime now) {
        return touchSessionActivityByStatus(id, now, TableSessionStatus.OPEN);
    }

    @Modifying
    @Query(
            """
            UPDATE TableSession session
            SET session.lastActivityAt = :now,
                session.updatedAt = :now
            WHERE session.id = :id
              AND session.status = :status
              AND session.isDeleted = false
            """)
    int touchSessionActivityByStatus(
            @Param("id") Long id, @Param("now") LocalDateTime now, @Param("status") TableSessionStatus status);

    default List<TableSession> findStaleOpenSessionsWithoutOrdersForUpdate(LocalDateTime cutoff) {
        return findStaleSessionsByStatusWithoutOrdersForUpdate(cutoff, TableSessionStatus.OPEN.name());
    }

    @Query(
            value =
                    """
            SELECT s.*
            FROM table_sessions s
            WHERE s.status = :status
              AND s.is_deleted = false
              AND s.last_activity_at < :cutoff
              AND NOT EXISTS (
                    SELECT 1
                    FROM orders o
                    WHERE o.table_session_id = s.id
                      AND o.is_deleted = false
              )
            FOR UPDATE SKIP LOCKED
            """,
            nativeQuery = true)
    List<TableSession> findStaleSessionsByStatusWithoutOrdersForUpdate(
            @Param("cutoff") LocalDateTime cutoff, @Param("status") String status);
}
