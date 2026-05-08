// src/main/java/com/sacmauquan/qrordering/repository/VoucherRepository.java
package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Voucher;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;

import java.util.*;
import java.time.*;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    // Tìm mã có thể sử dụng được ngay bây giờ
    @Query("SELECT v FROM Voucher v WHERE LOWER(v.code) = LOWER(:code) " +
            "AND v.active = true " +
            "AND v.validFrom <= :now AND v.validTo >= :now " +
            "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    Optional<Voucher> findValidVoucher(@Param("code") String code, @Param("now") LocalDateTime now);

    // Kiểm tra trùng mã
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    // Tìm kiếm và Phân trang
    Page<Voucher> findByCodeContainingIgnoreCase(String code, Pageable pageable);

    // Lấy danh sách mới nhất
    Page<Voucher> findAllByOrderByIdDesc(Pageable pageable);
}
