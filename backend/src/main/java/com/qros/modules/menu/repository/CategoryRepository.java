package com.qros.modules.menu.repository;

import com.qros.modules.menu.model.Category;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
