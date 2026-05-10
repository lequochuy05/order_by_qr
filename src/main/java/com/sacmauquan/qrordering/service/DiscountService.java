package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.DiscountResult;
import com.sacmauquan.qrordering.dto.VoucherRequest;
import com.sacmauquan.qrordering.dto.VoucherValidateResponse;
import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Voucher - Manages discount codes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final VoucherRepository voucherRepo;
    private final NotificationService notificationService;

    /**
     * Get all vouchers
     */
    @Cacheable(value = "vouchers", key = "'all_desc'")
    public List<Voucher> findAll() {
        return voucherRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Get voucher by id
     */
    public Voucher findById(@NonNull Long id) {
        return voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));
    }

    /**
     * Create voucher
     */
    @Transactional
    @CacheEvict(value = "vouchers", allEntries = true)
    public Voucher create(VoucherRequest req) {
        // Check if voucher code already exists
        if (voucherRepo.existsByCodeIgnoreCase(req.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher code already exists");
        }

        Voucher v = Voucher.builder()
                .code(req.getCode().trim().toUpperCase())
                .type(req.getType())
                .discountAmount(req.getDiscountAmount())
                .discountPercent(req.getDiscountPercent())
                .usageLimit(req.getUsageLimit())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .active(req.getActive() != null ? req.getActive() : true)
                .usedCount(0)
                .build();

        Voucher saved = voucherRepo.save(Objects.requireNonNull(v));
        notificationService.notifyVoucherChange();

        return saved;
    }

    /**
     * Update voucher
     */
    @Transactional
    @CacheEvict(value = "vouchers", allEntries = true)
    public Voucher update(@NonNull Long id, VoucherRequest req) {
        Voucher v = findById(id);

        // Check if voucher code already exists
        if (voucherRepo.existsByCodeIgnoreCaseAndIdNot(req.getCode(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher code already exists");
        }

        v.setCode(req.getCode().trim().toUpperCase());
        v.setType(req.getType());
        v.setDiscountAmount(req.getDiscountAmount());
        v.setDiscountPercent(req.getDiscountPercent());
        v.setUsageLimit(req.getUsageLimit());
        v.setValidFrom(req.getValidFrom());
        v.setValidTo(req.getValidTo());
        v.setActive(req.getActive() != null ? req.getActive() : v.getActive());

        Voucher saved = voucherRepo.save(Objects.requireNonNull(v));
        notificationService.notifyVoucherChange();

        return saved;
    }

    /**
     * Delete voucher
     */
    @Transactional
    @CacheEvict(value = "vouchers", allEntries = true)
    public void delete(@NonNull Long id) {
        Voucher v = findById(id);
        voucherRepo.delete(Objects.requireNonNull(v));
        notificationService.notifyVoucherChange();
    }

    /**
     * Validate and calculate discount
     */
    public VoucherValidateResponse validateCode(String code, BigDecimal orderTotal) {
        if (code == null || code.isBlank()) {
            return new VoucherValidateResponse(null, "NOT_FOUND", BigDecimal.ZERO, null, false);
        }

        String cleanCode = code.trim().toUpperCase();
        Voucher v = voucherRepo.findByCodeIgnoreCase(cleanCode).orElse(null);

        if (v == null) {
            return new VoucherValidateResponse(cleanCode, "NOT_FOUND", BigDecimal.ZERO, null, false);
        }

        String status = getVoucherStatus(v);
        boolean applicable = "ACTIVE".equals(status);
        BigDecimal discountValue = BigDecimal.ZERO;

        if (applicable) {
            discountValue = calculateDiscount(v, orderTotal);
        }

        return new VoucherValidateResponse(v.getCode(), status, discountValue, v.getDiscountPercent(), applicable);
    }

    /**
     * Apply voucher to order
     */
    @Transactional
    @CacheEvict(value = "vouchers", allEntries = true)
    public DiscountResult applyVoucher(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return new DiscountResult(subtotal, BigDecimal.ZERO, null);
        }

        // Get voucher
        Voucher v = voucherRepo.findByCodeIgnoreCase(code.trim().toUpperCase())
                .orElse(null);

        // If voucher not found, return result without discount
        if (v == null) {
            return new DiscountResult(subtotal, BigDecimal.ZERO, null);
        }

        // Check status
        String status = getVoucherStatus(v);
        if (!"ACTIVE".equals(status)) {
            return new DiscountResult(subtotal, BigDecimal.ZERO, null);
        }

        // Calculate discount value
        BigDecimal discountValue = calculateDiscount(v, subtotal);

        // Update used count
        int updatedRows = voucherRepo.incrementUsedCountAtomically(v.getId());
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is exhausted");
        }

        // Calculate final total
        BigDecimal finalTotal = subtotal.subtract(discountValue).setScale(2, RoundingMode.HALF_UP);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0)
            finalTotal = BigDecimal.ZERO;

        return new DiscountResult(finalTotal, discountValue, v);
    }

    /**
     * Get voucher status
     */
    private String getVoucherStatus(Voucher v) {
        if (!Boolean.TRUE.equals(v.getActive()))
            return "INACTIVE";

        LocalDateTime now = LocalDateTime.now();
        if (v.getValidFrom() != null && now.isBefore(v.getValidFrom()))
            return "UPCOMING";
        if (v.getValidTo() != null && now.isAfter(v.getValidTo()))
            return "EXPIRED";

        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit())
            return "EXHAUSTED";

        return "ACTIVE";
    }

    /**
     * Calculate discount value
     */
    private BigDecimal calculateDiscount(Voucher v, BigDecimal orderTotal) {
        if (v.getType() == Voucher.VoucherType.FIXED_AMOUNT) {
            return v.getDiscountAmount() != null ? v.getDiscountAmount() : BigDecimal.ZERO;
        } else if (v.getType() == Voucher.VoucherType.PERCENTAGE) {
            if (v.getDiscountPercent() == null || orderTotal == null)
                return BigDecimal.ZERO;
            return orderTotal.multiply(BigDecimal.valueOf(v.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
