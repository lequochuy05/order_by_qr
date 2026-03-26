package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.MenuItem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    @EntityGraph(attributePaths = { "category", "comboItems" })
    List<MenuItem> findByCategoryId(Integer cateId);

    @Override
    @EntityGraph(attributePaths = { "category", "comboItems" })
    List<MenuItem> findAll();

    boolean existsByNameIgnoreCase(String name);
}