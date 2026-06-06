package com.qros.modules.promotion.service;

import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.promotion.dto.DiscountResult;
import com.qros.modules.promotion.dto.VoucherRequest;
import com.qros.modules.promotion.dto.VoucherValidateResponse;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.repository.VoucherRepository;
import com.qros.shared.util.AppTime;
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
 * DiscountService - Manages the lifecycle of promotional vouchers and handles discount calculation logic.
 * Ensures atomic usage tracking and provides validation services for checkout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final VoucherRepository voucherRepo;
    private final NotificationService notificationService;

    /**
     * Retrieves all vouchers currently registered, sorted by newest first.
     * 
     * @return List of all vouchers
     */
    @Cacheable(value = "vouchers", key = "'all_desc'")
    public List<Voucher> findAll() {
        return voucherRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Finds a single voucher by its identifier.
     * 
     * @param id Voucher ID
     * @return Voucher entity
     * @throws ResponseStatusException if voucher not found
     */
    public Voucher findById(@NonNull Long id) {
        return voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));
    }

    /**
     * Creates a new promotional voucher.
     * 
     * @param req Voucher details
     * @return Created Voucher entity
     * @throws ResponseStatusException if the code already exists
     */
    @Transactional
    @CacheEvict(value = "vouchers", allEntries = true)
    public Voucher create(VoucherRequest req) {
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
     * Updates an existing voucher's properties.
     * 
     * @param id Voucher ID
     * @param req Update request
     * @return Updated Voucher entity
     */
    @Transactional
    @CacheEvict(value = "vouchers", allEntries = true)
    public Voucher update(@NonNull Long id, VoucherRequest req) {
        Voucher v = findById(id);

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
     * Permanently deletes a voucher and notifies system components.
     * 
     * @param id Voucher ID
     */
    @Transactional
    @CacheEvict(value = "vouchers", allEntries = true)
    public void delete(@NonNull Long id) {
        Voucher v = findById(id);
        voucherRepo.delete(Objects.requireNonNull(v));
        notificationService.notifyVoucherChange();
    }

    /**
     * Validates a discount code against current time and order total without applying it.
     * Used for real-time validation on the frontend cart.
     * 
     * @param code The input code
     * @param orderTotal Current subtotal of the cart
     * @return Validation response including status and calculated discount value
     */
    public VoucherValidateResponse validateCode(String code, BigDecimal orderTotal) {
        if (code == null || code.isBlank()) {
            return new VoucherValidateResponse(null, "NOT_FOUND", BigDecimal.ZERO, null, false);
        }

        String cleanCode = code.trim().toUpperCase();
        VoucherRepository.VoucherValidationProjection v = voucherRepo.findProjectedByCodeIgnoreCase(cleanCode).orElse(null);

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
     * Applies a voucher to an order, performing final validation and atomic usage increment.
     * 
     * @param code The voucher code to apply
     * @param subtotal The order subtotal
     * @return DiscountResult containing final total and applied discount value
     */
    @Transactional
    @CacheEvict(value = "vouchers", allEntries = true)
    public DiscountResult applyVoucher(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return new DiscountResult(subtotal, BigDecimal.ZERO, null);
        }

        Voucher v = voucherRepo.findByCodeIgnoreCase(code.trim().toUpperCase())
                .orElse(null);

        if (v == null) {
            return new DiscountResult(subtotal, BigDecimal.ZERO, null);
        }

        String status = getVoucherStatus(v);
        if (!"ACTIVE".equals(status)) {
            return new DiscountResult(subtotal, BigDecimal.ZERO, null);
        }

        BigDecimal discountValue = calculateDiscount(v, subtotal);

        // Attempt to increment usage count atomically to prevent race conditions
        int updatedRows = voucherRepo.incrementUsedCountAtomically(v.getId());
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher usage limit reached");
        }

        BigDecimal finalTotal = subtotal.subtract(discountValue).setScale(2, RoundingMode.HALF_UP);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0)
            finalTotal = BigDecimal.ZERO;

        return new DiscountResult(finalTotal, discountValue, v);
    }

    /**
     * Atomically increments the usage count of a voucher code.
     * Used when a payment is finalized (e.g., PayOS webhook).
     * 
     * @param code The voucher code to increment
     */
    @Transactional
    public void incrementUsage(String code) {
        if (code == null || code.isBlank())
            return;
        Voucher v = voucherRepo.findByCodeIgnoreCase(code.trim().toUpperCase())
                .orElse(null);
        if (v != null) {
            voucherRepo.incrementUsedCountAtomically(v.getId());
        }
    }

    /**
     * Evaluates the comprehensive status of a voucher based on activation, time, and usage.
     */
    private String getVoucherStatus(Voucher v) {
        if (!Boolean.TRUE.equals(v.getActive()))
            return "INACTIVE";

        LocalDateTime now = AppTime.now();
        if (v.getValidFrom() != null && now.isBefore(v.getValidFrom()))
            return "UPCOMING";
        if (v.getValidTo() != null && now.isAfter(v.getValidTo()))
            return "EXPIRED";

        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit())
            return "EXHAUSTED";

        return "ACTIVE";
    }

    /**
     * Core math logic for calculating discount values based on voucher type.
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

    private String getVoucherStatus(VoucherRepository.VoucherValidationProjection v) {
        if (!Boolean.TRUE.equals(v.getActive()))
            return "INACTIVE";

        LocalDateTime now = AppTime.now();
        if (v.getValidFrom() != null && now.isBefore(v.getValidFrom()))
            return "UPCOMING";
        if (v.getValidTo() != null && now.isAfter(v.getValidTo()))
            return "EXPIRED";

        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit())
            return "EXHAUSTED";

        return "ACTIVE";
    }

    private BigDecimal calculateDiscount(VoucherRepository.VoucherValidationProjection v, BigDecimal orderTotal) {
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
