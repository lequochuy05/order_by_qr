package com.sacmauquan.qrordering.repository;

import java.util.List;

import com.sacmauquan.qrordering.model.Combo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = { "items.menuItem.category" })
    List<Combo> findByActiveTrue();

    @Override
    @EntityGraph(attributePaths = { "items.menuItem.category" })
    List<Combo> findAll();
}
