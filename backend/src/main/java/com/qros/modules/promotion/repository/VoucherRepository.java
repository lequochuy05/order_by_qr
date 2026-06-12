package com.qros.modules.promotion.repository;

import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.repository.projection.VoucherValidationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<VoucherValidationProjection> findProjectedByCodeIgnoreCase(String code);

    @Query("""
            SELECT v
            FROM Voucher v
            WHERE LOWER(v.code) = LOWER(:code)
              AND v.active = true
              AND v.validFrom <= :now
              AND v.validTo >= :now
              AND (
                    v.usageLimit IS NULL
                    OR v.usedCount < v.usageLimit
                  )
            """)
    Optional<Voucher> findValidVoucher(
            @Param("code") String code,
            @Param("now") LocalDateTime now);

    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Page<Voucher> findByCodeContainingIgnoreCase(String code, Pageable pageable);

    Page<Voucher> findAllByOrderByIdDesc(Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Voucher v
            SET v.usedCount = v.usedCount + 1
            WHERE v.id = :id
              AND (
                    v.usageLimit IS NULL
                    OR v.usedCount < v.usageLimit
                  )
            """)
    int incrementUsedCountAtomically(@Param("id") Long id);
}