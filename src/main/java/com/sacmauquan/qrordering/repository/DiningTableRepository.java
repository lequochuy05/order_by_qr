package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    boolean existsByTableNumber(String tableNumber);

    boolean existsByTableNumberAndIdNot(String tableNumber, Long id);

    Optional<DiningTable> findByTableNumber(String tableNumber);

    Optional<DiningTable> findByTableCode(String tableCode);

    List<DiningTable> findByStatus(DiningTable.TableStatus status);

    List<DiningTable> findAllByOrderByTableNumberAsc();
}
