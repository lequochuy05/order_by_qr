package com.qros.modules.menu.repository;

import com.qros.modules.menu.model.Combo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ComboRepository extends JpaRepository<Combo, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query(
            """
        SELECT c
        FROM Combo c
        WHERE (CAST(:keyword AS string) IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          AND (CAST(:active AS boolean) IS NULL OR c.active = :active)
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    Page<Combo> searchManagementSummaries(
            @Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);

    @Query(
            """
        SELECT DISTINCT c
        FROM Combo c
        LEFT JOIN FETCH c.items ci
        LEFT JOIN FETCH ci.menuItem mi
        LEFT JOIN FETCH mi.category
        WHERE c.id = :id
    """)
    Optional<Combo> findByIdWithItems(@Param("id") Long id);

    @Query(
            """
        SELECT DISTINCT c
        FROM Combo c
        LEFT JOIN FETCH c.items ci
        LEFT JOIN FETCH ci.menuItem mi
        LEFT JOIN FETCH mi.category
        WHERE c.id IN :ids
    """)
    List<Combo> findAllByIdInWithItems(@Param("ids") List<Long> ids);

    @Query(
            """
        SELECT DISTINCT c
        FROM Combo c
        LEFT JOIN FETCH c.items ci
        LEFT JOIN FETCH ci.menuItem mi
        LEFT JOIN FETCH mi.category
        WHERE c.active = true
          AND c.available = true
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    List<Combo> findAllActiveWithItems();

    @Query(
            """
        SELECT DISTINCT c
        FROM Combo c
        LEFT JOIN FETCH c.items ci
        LEFT JOIN FETCH ci.menuItem mi
        LEFT JOIN FETCH mi.category
        WHERE c.active = true
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    List<Combo> findAllPublicWithItems();
}
