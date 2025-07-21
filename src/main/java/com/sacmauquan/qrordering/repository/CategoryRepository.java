package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}