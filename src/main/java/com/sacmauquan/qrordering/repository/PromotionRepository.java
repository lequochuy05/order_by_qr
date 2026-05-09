package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Promotion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // Lấy tất cả chương trình khuyến mãi đang có hiệu lực
    @Query("SELECT p FROM Promotion p WHERE p.active = true " +
            "AND p.startTime <= :now AND p.endTime >= :now")
    List<Promotion> findAllActive(@Param("now") LocalTime now);

    // Kiểm tra trùng tên
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Tìm kiếm phân trang
    Page<Promotion> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
