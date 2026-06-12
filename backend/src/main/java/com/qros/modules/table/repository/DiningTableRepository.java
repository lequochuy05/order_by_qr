package com.qros.modules.table.repository;

import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.enums.TableStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    boolean existsByTableNumberIgnoreCase(String tableNumber);

    boolean existsByTableNumberIgnoreCaseAndIdNot(String tableNumber, Long id);

    boolean existsByTableCode(String tableCode);

    Optional<DiningTable> findByTableNumberIgnoreCase(String tableNumber);

    Optional<DiningTable> findByTableCode(String tableCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM DiningTable t WHERE t.id = :id")
    Optional<DiningTable> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM DiningTable t WHERE t.tableCode = :tableCode")
    Optional<DiningTable> findByTableCodeForUpdate(@Param("tableCode") String tableCode);

    List<DiningTable> findByStatusOrderByTableNumberAsc(TableStatus status);

    List<DiningTable> findAllByOrderByTableNumberAsc();
}
