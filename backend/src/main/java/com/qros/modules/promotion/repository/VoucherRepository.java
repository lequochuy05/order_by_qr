package com.qros.modules.promotion.repository;

import com.qros.modules.promotion.model.Voucher;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;

import java.util.*;
import java.time.*;

/**
 * VoucherRepository - Repository interface for managing Voucher entities.
 * Includes logic for validating discount codes and atomic usage tracking.
 */
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    
    interface VoucherValidationProjection {
        String getCode();
        Voucher.VoucherType getType();
        java.math.BigDecimal getDiscountAmount();
        Double getDiscountPercent();
        Integer getUsageLimit();
        Integer getUsedCount();
        LocalDateTime getValidFrom();
        LocalDateTime getValidTo();
        Boolean getActive();
    }

    /**
     * Finds a lightweight projection of a voucher by code.
     */
    Optional<VoucherValidationProjection> findProjectedByCodeIgnoreCase(String code);

    /**
     * Finds a voucher by code that is currently active, within its validity period, and hasn't exceeded its usage limit.
     * 
     * @param code The voucher code string
     * @param now Current timestamp for validity check
     * @return Optional containing the valid voucher
     */
    @Query("SELECT v FROM Voucher v WHERE LOWER(v.code) = LOWER(:code) " +
            "AND v.active = true " +
            "AND v.validFrom <= :now AND v.validTo >= :now " +
            "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    Optional<Voucher> findValidVoucher(@Param("code") String code, @Param("now") LocalDateTime now);

    /**
     * Finds a voucher by its code (case-insensitive).
     * 
     * @param code The target code
     * @return Optional containing the voucher
     */
    Optional<Voucher> findByCodeIgnoreCase(String code);

    /**
     * Checks if a voucher code already exists (case-insensitive).
     * 
     * @param code The code to check
     * @return true if exists, false otherwise
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Checks if another voucher already uses this code (case-insensitive).
     * 
     * @param code The code to check
     * @param id The ID to exclude from search
     * @return true if another voucher uses the same code
     */
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /**
     * Searches for vouchers by code partial match with pagination.
     * 
     * @param code The search keyword
     * @param pageable Pagination and sorting information
     * @return Paged result of matching vouchers
     */
    Page<Voucher> findByCodeContainingIgnoreCase(String code, Pageable pageable);

    /**
     * Retrieves all vouchers sorted by ID in descending order.
     * 
     * @param pageable Pagination and sorting information
     * @return Paged result of vouchers
     */
    Page<Voucher> findAllByOrderByIdDesc(Pageable pageable);

    /**
     * Atomically increments the usage count of a voucher if the usage limit has not been reached.
     * 
     * @param id The voucher identifier
     * @return Number of rows updated (should be 1 if successful)
     */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 " +
           "WHERE v.id = :id AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    int incrementUsedCountAtomically(@Param("id") Long id);
}
