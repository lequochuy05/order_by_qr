package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Category;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

  // Kiểm tra trùng tên khi tạo mới
  boolean existsByNameIgnoreCase(String name);

  // Kiểm tra trùng tên khi cập nhật
  boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

  @Query(value = "SELECT COUNT(*) > 0 FROM category WHERE LOWER(name) = LOWER(:name)", nativeQuery = true)
  boolean existsByNameIncludingDeleted(String name);

  @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.menuItems m " +
      "LEFT JOIN FETCH m.itemOptions io " +
      "LEFT JOIN FETCH io.optionValues " +
      "WHERE c.active = true AND (m.active = true OR m.active IS NULL)")
  List<Category> findAllActiveWithItems();

  Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
