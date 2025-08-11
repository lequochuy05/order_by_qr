package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    boolean existsByTableNumber(String tableNumber);
    Optional<DiningTable> findByTableNumber(String tableNumber);
}