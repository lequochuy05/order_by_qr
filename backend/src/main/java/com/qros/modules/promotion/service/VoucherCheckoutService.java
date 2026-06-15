package com.qros.modules.promotion.service;

import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.dto.internal.VoucherPaymentResult;
import com.qros.modules.promotion.dto.response.VoucherValidateResponse;
import com.qros.modules.promotion.mapper.VoucherMapper;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.model.enums.VoucherValidationStatus;
import com.qros.modules.promotion.repository.VoucherRepository;
import com.qros.modules.promotion.repository.projection.VoucherValidationProjection;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherCheckoutService {

    private final VoucherRepository voucherRepository;
    private final VoucherMapper voucherMapper;
    private final DiscountCalculator discountCalculator;

    @Transactional(readOnly = true)
    public VoucherValidateResponse validateCode(String code, BigDecimal subtotal) {
        String normalizedCode = normalizeNullableCode(code);
        BigDecimal safeSubtotal = subtotal != null ? subtotal : BigDecimal.ZERO;

        if (normalizedCode == null) {
            return voucherMapper.notFoundValidateResponse(null, safeSubtotal);
        }

        VoucherValidationProjection voucher = voucherRepository.findProjectedByCodeIgnoreCase(normalizedCode)
                .orElse(null);

        if (voucher == null) {
            return voucherMapper.notFoundValidateResponse(normalizedCode, safeSubtotal);
        }

        VoucherValidationStatus status = resolveStatus(voucher);
        boolean applicable = status == VoucherValidationStatus.ACTIVE;

        DiscountResult result = applicable
                ? discountCalculator.calculate(voucher, safeSubtotal)
                : discountCalculator.noDiscount(safeSubtotal);

        return applicable
                ? voucherMapper.toValidateResponse(voucher, status, result, true)
                : voucherMapper.notApplicableValidateResponse(voucher, status, result);
    }

    @Transactional(readOnly = true)
    public DiscountResult previewVoucher(String code, BigDecimal subtotal) {
        String normalizedCode = normalizeNullableCode(code);

        if (normalizedCode == null) {
            return discountCalculator.noDiscount(subtotal);
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .orElse(null);

        if (voucher == null || resolveStatus(voucher) != VoucherValidationStatus.ACTIVE) {
            return discountCalculator.noDiscount(subtotal);
        }

        return discountCalculator.calculate(voucher, subtotal);
    }

    @Transactional(readOnly = true)
    public Voucher getActiveVoucherForPayment(String code) {
        String normalizedCode = normalizeNullableCode(code);

        if (normalizedCode == null) {
            return null;
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        VoucherValidationStatus status = resolveStatus(voucher);
        throwIfVoucherNotActiveForPayment(status);

        return voucher;
    }

    @Transactional(readOnly = true)
    public VoucherPaymentResult resolveForPayment(String code, BigDecimal subtotal) {
        String normalizedCode = normalizeNullableCode(code);
        BigDecimal safeSubtotal = subtotal != null ? subtotal : BigDecimal.ZERO;

        if (normalizedCode == null) {
            return new VoucherPaymentResult(null, discountCalculator.noDiscount(safeSubtotal));
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        VoucherValidationStatus status = resolveStatus(voucher);
        throwIfVoucherNotActiveForPayment(status);

        return new VoucherPaymentResult(
                voucher,
                discountCalculator.calculate(voucher, safeSubtotal));
    }

    @Transactional(readOnly = true)
    public VoucherPaymentResult snapshotForSettledOrder(
            String code,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal finalAmount) {
        String normalizedCode = normalizeNullableCode(code);

        if (normalizedCode == null) {
            return null;
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        DiscountResult result = new DiscountResult(
                voucher.getId(),
                voucher.getCode(),
                voucher.getType(),
                subtotal != null ? subtotal : BigDecimal.ZERO,
                voucher.getDiscountAmount(),
                voucher.getDiscountPercent(),
                discountAmount != null ? discountAmount : BigDecimal.ZERO,
                finalAmount != null ? finalAmount : BigDecimal.ZERO);

        return new VoucherPaymentResult(voucher, result);
    }

    private void throwIfVoucherNotActiveForPayment(VoucherValidationStatus status) {
        if (status == VoucherValidationStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VOUCHER_INACTIVE);
        }

        if (status == VoucherValidationStatus.NOT_YET_ACTIVE) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_YET_ACTIVE);
        }

        if (status == VoucherValidationStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.VOUCHER_EXPIRED);
        }

        if (status == VoucherValidationStatus.EXHAUSTED) {
            throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
        }
    }

    private VoucherValidationStatus resolveStatus(Voucher voucher) {
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            return VoucherValidationStatus.INACTIVE;
        }

        LocalDateTime now = AppTime.now();

        if (voucher.getValidFrom() != null && now.isBefore(voucher.getValidFrom())) {
            return VoucherValidationStatus.NOT_YET_ACTIVE;
        }

        if (voucher.getValidTo() != null && now.isAfter(voucher.getValidTo())) {
            return VoucherValidationStatus.EXPIRED;
        }

        if (voucher.getUsageLimit() != null
                && voucher.getUsageLimit() > 0
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return VoucherValidationStatus.EXHAUSTED;
        }

        return VoucherValidationStatus.ACTIVE;
    }

    private VoucherValidationStatus resolveStatus(VoucherValidationProjection voucher) {
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            return VoucherValidationStatus.INACTIVE;
        }

        LocalDateTime now = AppTime.now();

        if (voucher.getValidFrom() != null && now.isBefore(voucher.getValidFrom())) {
            return VoucherValidationStatus.NOT_YET_ACTIVE;
        }

        if (voucher.getValidTo() != null && now.isAfter(voucher.getValidTo())) {
            return VoucherValidationStatus.EXPIRED;
        }

        if (voucher.getUsageLimit() != null
                && voucher.getUsageLimit() > 0
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return VoucherValidationStatus.EXHAUSTED;
        }

        return VoucherValidationStatus.ACTIVE;
    }

    private String normalizeNullableCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return code.trim().toUpperCase();
    }
}
