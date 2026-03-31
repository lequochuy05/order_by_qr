package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.MenuItem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    @EntityGraph(attributePaths = { "category", "comboItems", "itemOptions", "itemOptions.optionValues" })
    List<MenuItem> findByCategoryId(Integer cateId);

    @Override
    @EntityGraph(attributePaths = { "category", "comboItems", "itemOptions", "itemOptions.optionValues" })
    List<MenuItem> findAll();

    @EntityGraph(attributePaths = { "category", "comboItems", "itemOptions", "itemOptions.optionValues" })
    Optional<MenuItem> findById(Long id);

    boolean existsByNameIgnoreCase(String name);
}