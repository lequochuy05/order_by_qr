package com.qros.modules.menu.repository;

import com.qros.modules.menu.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

  boolean existsByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

  @EntityGraph(attributePaths = {"menuItems"})
  List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();

  @EntityGraph(attributePaths = {"menuItems"})
  Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

}