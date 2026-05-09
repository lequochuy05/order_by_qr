package com.sacmauquan.qrordering.repository;

import java.util.*;

import org.springframework.data.domain.*;

import com.sacmauquan.qrordering.model.Combo;

import org.springframework.data.jpa.repository.*;
import org.springframework.lang.NonNull;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    // Kiểm tra trùng tên khi tạo mới
    boolean existsByNameIgnoreCase(String name);

    // Kiểm tra trùng tên khi cập nhật
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Override
    @EntityGraph(attributePaths = { "items.menuItem.category" })
    @NonNull
    List<Combo> findAll();

    // Load Menu khách hàng
    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.items " +
            "WHERE c.active = true")
    List<Combo> findAllActiveWithItems();

}
