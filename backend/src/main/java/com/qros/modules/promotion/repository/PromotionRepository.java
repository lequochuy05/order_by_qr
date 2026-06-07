package com.qros.modules.promotion.repository;

import com.qros.modules.promotion.model.Promotion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("SELECT p FROM Promotion p WHERE p.active = true " +
            "AND p.startTime <= :now AND p.endTime >= :now " +
            "AND (p.daysOfWeek IS NULL OR p.daysOfWeek LIKE %:dayOfWeek%)")
    List<Promotion> findAllActive(@Param("now") LocalTime now, @Param("dayOfWeek") String dayOfWeek);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<Promotion> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
