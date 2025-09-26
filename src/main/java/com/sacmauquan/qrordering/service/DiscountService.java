package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.DiscountResult;
import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.repository.VoucherRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DiscountService {

    private final VoucherRepository voucherRepository;

    public DiscountService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    /**
     * Tính tổng sau giảm. Nếu increaseUsage=true thì tăng used_count (dùng khi PAY).
     */
    public DiscountResult applyDiscounts(double subtotal, String voucherCode, boolean increaseUsage) {
        double finalTotal = subtotal;
        double discountValue = 0d;
        Voucher appliedVoucher = null;

        if (voucherCode != null && !voucherCode.isBlank()) {
            Voucher v = voucherRepository.findByCode(voucherCode.trim())
                    .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

            LocalDateTime now = LocalDateTime.now();
            Integer used = Optional.ofNullable(v.getUsedCount()).orElse(0);
            Integer limit = Optional.ofNullable(v.getUsageLimit()).orElse(Integer.MAX_VALUE);

            boolean timeOk = (v.getValidFrom() == null || !now.isBefore(v.getValidFrom()))
                    && (v.getValidTo() == null || !now.isAfter(v.getValidTo()));
            boolean canUse = Boolean.TRUE.equals(v.getActive()) && used < limit && timeOk;

            if (canUse) {
                if (v.getDiscountPercent() != null) {
                    discountValue = subtotal * v.getDiscountPercent() / 100.0;
                } else if (v.getDiscountAmount() != null) {
                    discountValue = v.getDiscountAmount();
                }
                finalTotal = Math.max(0d, subtotal - discountValue);
                appliedVoucher = v;

                if (increaseUsage) {
                    v.setUsedCount(used + 1);
                    voucherRepository.save(v);
                }
            }
        }

        return new DiscountResult(finalTotal, discountValue, appliedVoucher);
    }

    /** Dùng để kiểm tra hiển thị ở màn preview. */
    public VoucherResult validateVoucher(String code, double subtotal) {
        if (code == null || code.isBlank()) {
            return new VoucherResult(false, "Không có mã giảm giá", 0d);
        }

        Optional<Voucher> opt = voucherRepository.findByCodeAndActiveTrue(code.trim());
        if (opt.isEmpty()) {
            return new VoucherResult(false, "Mã không hợp lệ hoặc đã hết hạn", 0d);
        }

        Voucher v = opt.get();
        LocalDateTime now = LocalDateTime.now();
        Integer used = Optional.ofNullable(v.getUsedCount()).orElse(0);
        Integer limit = Optional.ofNullable(v.getUsageLimit()).orElse(Integer.MAX_VALUE);

        boolean timeOk = (v.getValidFrom() == null || !now.isBefore(v.getValidFrom()))
                && (v.getValidTo() == null || !now.isAfter(v.getValidTo()));
        boolean canUse = Boolean.TRUE.equals(v.getActive()) && used < limit && timeOk;

        if (!canUse) {
            return new VoucherResult(false, "Mã không hợp lệ hoặc đã hết hạn", 0d);
        }

        double discount = 0d;
        if (v.getDiscountPercent() != null) {
            discount = subtotal * (v.getDiscountPercent() / 100.0);
        } else if (v.getDiscountAmount() != null) {
            discount = v.getDiscountAmount();
        }

        return new VoucherResult(true, "Áp dụng thành công", discount);
    }

    @Getter @AllArgsConstructor
    public static class VoucherResult {
        private boolean valid;
        private String message;
        private double discount;
    }
}
