package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Voucher;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;

import java.util.*;
import java.time.*;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    @Query("SELECT v FROM Voucher v WHERE LOWER(v.code) = LOWER(:code) " +
            "AND v.active = true " +
            "AND v.validFrom <= :now AND v.validTo >= :now " +
            "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    Optional<Voucher> findValidVoucher(@Param("code") String code, @Param("now") LocalDateTime now);

    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Page<Voucher> findByCodeContainingIgnoreCase(String code, Pageable pageable);

    Page<Voucher> findAllByOrderByIdDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 " +
           "WHERE v.id = :id AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    int incrementUsedCountAtomically(@Param("id") Long id);
}
