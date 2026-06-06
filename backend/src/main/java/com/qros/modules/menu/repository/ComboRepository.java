package com.qros.modules.menu.repository;

import java.util.*;

import com.qros.modules.menu.model.Combo;

import org.springframework.data.jpa.repository.*;
import org.springframework.lang.NonNull;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query(value = "SELECT COUNT(*) > 0 FROM combos WHERE LOWER(name) = LOWER(:name)", nativeQuery = true)
    boolean existsByNameIncludingDeleted(String name);

    @Override
    @EntityGraph(attributePaths = { "items.menuItem.category" })
    @NonNull
    List<Combo> findAll();

    @Query("SELECT DISTINCT c FROM Combo c " +
            "LEFT JOIN FETCH c.items ci " +
            "LEFT JOIN FETCH ci.menuItem mi " +
            "LEFT JOIN FETCH mi.category " +
            "WHERE c.active = true")
    List<Combo> findAllActiveWithItems();

}
