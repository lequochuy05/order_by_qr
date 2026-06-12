package com.qros.modules.promotion.repository;

import com.qros.modules.promotion.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("""
            SELECT DISTINCT p
            FROM Promotion p
            WHERE p.active = true
              AND p.startTime <= :now
              AND p.endTime >= :now
              AND (
                    p.daysOfWeek IS EMPTY
                    OR :dayOfWeek MEMBER OF p.daysOfWeek
                  )
            """)
    List<Promotion> findAllActive(
            @Param("now") LocalTime now,
            @Param("dayOfWeek") DayOfWeek dayOfWeek);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<Promotion> findByNameContainingIgnoreCase(String name, Pageable pageable);
}