package com.sacmauquan.qrordering.repository;

import java.util.List;
import org.springframework.data.domain.*;

import com.sacmauquan.qrordering.model.Combo;

import org.springframework.data.jpa.repository.*;
import org.springframework.lang.NonNull;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    // Kiểm tra trùng tên khi tạo mới
    boolean existsByNameIgnoreCase(String name);

    // Kiểm tra trùng tên khi cập nhật
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Load Menu khách hàng
    @EntityGraph(attributePaths = { "items.menuItem.category" })
    List<Combo> findByActiveTrue();

    // Tìm kiếm phân trang
    @EntityGraph(attributePaths = { "items.menuItem.category" })
    Page<Combo> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "items.menuItem.category" })
    @NonNull
    List<Combo> findAll();
}
