package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.MenuItem;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    // Load món ăn theo danh mục kèm đầy đủ Options
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    List<MenuItem> findByCategoryIdAndActiveTrue(Integer cateId);

    // Kiểm tra trùng tên
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Tìm kiếm phân trang kèm theo đầy đủ thông tin
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    Page<MenuItem> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    @NonNull
    List<MenuItem> findAll();

    @Override
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    @NonNull
    Optional<MenuItem> findById(@NonNull Long id);

    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    List<MenuItem> findAllByActiveTrue();
}
