package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

  boolean existsByNameIgnoreCase(String name);

  Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
