package com.qros.modules.table.repository;

import com.qros.modules.table.model.TableSessionTable;
import com.qros.modules.table.model.enums.TableSessionStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableSessionTableRepository extends JpaRepository<TableSessionTable, Long> {

    @EntityGraph(attributePaths = {"session", "session.table", "table"})
    @Query(
            """
            SELECT membership
            FROM TableSessionTable membership
            WHERE membership.table.id = :tableId
              AND membership.releasedAt IS NULL
              AND membership.session.status = :status
            """)
    Optional<TableSessionTable> findActiveOpenMembershipByTableId(
            @Param("tableId") Long tableId, @Param("status") TableSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"session", "session.table", "table"})
    @Query(
            """
            SELECT membership
            FROM TableSessionTable membership
            WHERE membership.table.id = :tableId
              AND membership.releasedAt IS NULL
              AND membership.session.status = :status
            """)
    Optional<TableSessionTable> findActiveOpenMembershipByTableIdForUpdate(
            @Param("tableId") Long tableId, @Param("status") TableSessionStatus status);

    @EntityGraph(attributePaths = {"session", "session.table", "table"})
    @Query(
            """
            SELECT membership
            FROM TableSessionTable membership
            WHERE membership.session.id = :sessionId
              AND membership.releasedAt IS NULL
            ORDER BY membership.primaryTable DESC, membership.table.tableNumber ASC
            """)
    List<TableSessionTable> findActiveBySessionId(@Param("sessionId") Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"session", "session.table", "table"})
    @Query(
            """
            SELECT membership
            FROM TableSessionTable membership
            WHERE membership.session.id = :sessionId
              AND membership.releasedAt IS NULL
            ORDER BY membership.primaryTable DESC, membership.table.id ASC
            """)
    List<TableSessionTable> findActiveBySessionIdForUpdate(@Param("sessionId") Long sessionId);

    @EntityGraph(attributePaths = {"session", "session.table", "table"})
    @Query(
            """
            SELECT membership
            FROM TableSessionTable membership
            WHERE membership.session.status = :status
              AND membership.releasedAt IS NULL
            """)
    List<TableSessionTable> findActiveBySessionStatus(@Param("status") TableSessionStatus status);

    @Query(
            """
            SELECT membership.table.id
            FROM TableSessionTable membership
            WHERE membership.session.id = :sessionId
              AND membership.releasedAt IS NULL
            ORDER BY membership.table.id ASC
            """)
    List<Long> findActiveTableIdsBySessionId(@Param("sessionId") Long sessionId);

    @Query(
            """
            SELECT COUNT(membership) > 0
            FROM TableSessionTable membership
            WHERE membership.table.id = :tableId
              AND membership.releasedAt IS NULL
              AND membership.session.status = :status
            """)
    boolean existsActiveOpenMembershipByTableId(
            @Param("tableId") Long tableId, @Param("status") TableSessionStatus status);

    @Query(
            """
            SELECT COUNT(membership) > 0
            FROM TableSessionTable membership
            WHERE membership.session.id = :sessionId
              AND membership.table.tableCode = :tableCode
              AND membership.releasedAt IS NULL
            """)
    boolean existsActiveBySessionIdAndTableCode(
            @Param("sessionId") Long sessionId, @Param("tableCode") String tableCode);

    @Query(
            """
            SELECT membership
            FROM TableSessionTable membership
            WHERE membership.session.id = :sessionId
              AND membership.primaryTable = true
              AND membership.releasedAt IS NULL
            """)
    Optional<TableSessionTable> findActivePrimaryBySessionId(@Param("sessionId") Long sessionId);

    @Query(
            """
            SELECT DISTINCT membership.session.id
            FROM TableSessionTable membership
            WHERE membership.table.id IN :tableIds
              AND membership.releasedAt IS NULL
              AND membership.session.status = :status
            """)
    List<Long> findActiveOpenSessionIdsByTableIds(
            @Param("tableIds") Collection<Long> tableIds, @Param("status") TableSessionStatus status);
}
