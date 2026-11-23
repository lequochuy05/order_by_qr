package com.qros.modules.promotion.repository;

import com.qros.modules.promotion.model.Promotion;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    List<Promotion> findAllActive(@Param("now") LocalTime now, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT DISTINCT p FROM Promotion p LEFT JOIN FETCH p.daysOfWeek")
    List<Promotion> findAllWithDays();

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<Promotion> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("""
            SELECT p
            FROM Promotion p
            WHERE (CAST(:keyword AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (
                    CAST(:status AS string) IS NULL
                    OR (
                        :status = 'ACTIVE'
                        AND p.active = true
                    )
                    OR (
                        :status = 'INACTIVE'
                        AND p.active = false
                    )
                    OR (
                        :status = 'RUNNING'
                        AND p.active = true
                        AND p.startTime <= :nowTime
                        AND p.endTime >= :nowTime
                        AND (
                            p.daysOfWeek IS EMPTY
                            OR :dayOfWeek MEMBER OF p.daysOfWeek
                        )
                    )
                    OR (
                        :status = 'SCHEDULED'
                        AND p.active = true
                        AND (
                            p.startTime > :nowTime
                            OR p.endTime < :nowTime
                            OR (
                                p.daysOfWeek IS NOT EMPTY
                                AND :dayOfWeek NOT MEMBER OF p.daysOfWeek
                            )
                        )
                    )
                  )
            """)
    Page<Promotion> searchForManagement(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("nowTime") LocalTime nowTime,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            Pageable pageable);
}
