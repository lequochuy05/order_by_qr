package com.qros.modules.promotion.service;

import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.model.enums.VoucherType;
import com.qros.modules.promotion.repository.projection.VoucherValidationProjection;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DiscountCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;

    public DiscountResult calculate(Voucher voucher, BigDecimal subtotal) {
        BigDecimal safeSubtotal = normalizeMoney(subtotal);

        if (voucher == null) {
            return noDiscount(safeSubtotal);
        }

        BigDecimal appliedDiscountAmount = calculateAppliedDiscount(
                voucher.getType(),
                voucher.getDiscountAmount(),
                voucher.getDiscountPercent(),
                safeSubtotal);

        BigDecimal finalAmount = calculateFinalAmount(safeSubtotal, appliedDiscountAmount);

        return new DiscountResult(
                voucher.getId(),
                voucher.getCode(),
                voucher.getType(),
                safeSubtotal,
                voucher.getDiscountAmount(),
                voucher.getDiscountPercent(),
                appliedDiscountAmount,
                finalAmount);
    }

    public DiscountResult calculate(VoucherValidationProjection voucher, BigDecimal subtotal) {
        BigDecimal safeSubtotal = normalizeMoney(subtotal);

        if (voucher == null) {
            return noDiscount(safeSubtotal);
        }

        BigDecimal appliedDiscountAmount = calculateAppliedDiscount(
                voucher.getType(),
                voucher.getDiscountAmount(),
                voucher.getDiscountPercent(),
                safeSubtotal);

        BigDecimal finalAmount = calculateFinalAmount(safeSubtotal, appliedDiscountAmount);

        return new DiscountResult(
                voucher.getId(),
                voucher.getCode(),
                voucher.getType(),
                safeSubtotal,
                voucher.getDiscountAmount(),
                voucher.getDiscountPercent(),
                appliedDiscountAmount,
                finalAmount);
    }

    public BigDecimal calculateAppliedDiscount(
            VoucherType type,
            BigDecimal discountAmount,
            BigDecimal discountPercent,
            BigDecimal subtotal) {
        BigDecimal safeSubtotal = normalizeMoney(subtotal);

        if (safeSubtotal.compareTo(ZERO) <= 0 || type == null) {
            return ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal rawDiscount = switch (type) {
            case FIXED_AMOUNT -> discountAmount != null ? discountAmount : ZERO;
            case PERCENTAGE -> {
                if (discountPercent == null) {
                    yield ZERO;
                }

                yield safeSubtotal
                        .multiply(discountPercent)
                        .divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
            }
        };

        if (rawDiscount.compareTo(ZERO) <= 0) {
            return ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        if (rawDiscount.compareTo(safeSubtotal) > 0) {
            return safeSubtotal;
        }

        return rawDiscount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFinalAmount(BigDecimal subtotal, BigDecimal appliedDiscountAmount) {
        BigDecimal safeSubtotal = normalizeMoney(subtotal);
        BigDecimal safeDiscount = normalizeMoney(appliedDiscountAmount);

        BigDecimal finalAmount = safeSubtotal.subtract(safeDiscount);

        if (finalAmount.compareTo(ZERO) < 0) {
            return ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        return finalAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public DiscountResult noDiscount(BigDecimal subtotal) {
        BigDecimal safeSubtotal = normalizeMoney(subtotal);

        return new DiscountResult(
                null,
                null,
                null,
                safeSubtotal,
                null,
                null,
                ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                safeSubtotal);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null || value.compareTo(ZERO) < 0) {
            return ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}