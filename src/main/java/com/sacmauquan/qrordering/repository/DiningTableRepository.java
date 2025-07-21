package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    DiningTable findByTableNumber(String tableNumber);
}