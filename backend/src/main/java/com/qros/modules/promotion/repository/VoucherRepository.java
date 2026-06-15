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



    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Page<Voucher> findByCodeContainingIgnoreCase(String code, Pageable pageable);

    Page<Voucher> findAllByOrderByIdDesc(Pageable pageable);

    @Query("""
            SELECT v
            FROM Voucher v
            WHERE (CAST(:keyword AS string) IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (
                    CAST(:status AS string) IS NULL
                    OR (
                        :status = 'ACTIVE'
                        AND v.active = true
                        AND v.validFrom <= :now
                        AND v.validTo >= :now
                        AND (
                            v.usageLimit IS NULL
                            OR v.usageLimit = 0
                            OR v.usedCount < v.usageLimit
                        )
                    )
                    OR (
                        :status = 'INACTIVE'
                        AND (
                            v.active = false
                            OR v.validFrom > :now
                        )
                    )
                    OR (
                        :status = 'EXPIRED'
                        AND v.active = true
                        AND v.validTo < :now
                    )
                    OR (
                        :status = 'EXHAUSTED'
                        AND v.active = true
                        AND v.validFrom <= :now
                        AND v.validTo >= :now
                        AND v.usageLimit IS NOT NULL
                        AND v.usageLimit > 0
                        AND v.usedCount >= v.usageLimit
                    )
                  )
            """)
    Page<Voucher> searchForManagement(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Voucher v
            SET v.usedCount = v.usedCount + 1
            WHERE v.id = :id
              AND (
                    v.usageLimit IS NULL
                    OR v.usageLimit = 0
                    OR v.usedCount < v.usageLimit
                  )
            """)
    int incrementUsedCountAtomically(@Param("id") Long id);
}
