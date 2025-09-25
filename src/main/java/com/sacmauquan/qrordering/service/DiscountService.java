package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.Promotion;
import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.repository.PromotionRepository;
import com.sacmauquan.qrordering.repository.VoucherRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VoucherRepository voucherRepo;
    private final PromotionRepository promoRepo;

    /**
     * Áp dụng tất cả loại giảm giá (combo/voucher/giờ vàng) và trả về tổng tiền sau giảm.
     * Ghi chú:
     * - Subtotal = sum(unitPrice * quantity) hiện tại của order.
     * - Nếu cần hỗ trợ combo thực sự, bạn thay đổi subtotal tại đây theo combo.
     */
    public double applyDiscounts(Order order, String voucherCode) {
        if (order == null || order.getOrderItems() == null) return 0d;

        // 1) Subtotal
        double subtotal = order.getOrderItems().stream()
                .mapToDouble(it -> safeDouble(it.getUnitPrice()) * it.getQuantity())
                .sum();

        double total = subtotal;

        // 2) Voucher (nếu có)
        if (voucherCode != null && !voucherCode.isBlank()) {
            Optional<Voucher> opt = voucherRepo.findByCodeAndActiveTrue(voucherCode.trim());
            if (opt.isPresent()) {
                Voucher v = opt.get();
                if (isVoucherValid(v)) {
                    double discount = 0d;
                    if (v.getDiscountPercent() != null) {
                        discount += subtotal * (v.getDiscountPercent() / 100.0);
                    }
                    if (v.getDiscountAmount() != null) {
                        discount += v.getDiscountAmount();
                    }
                    total -= discount;

                    // cập nhật đếm số lần dùng
                    v.setUsedCount(Optional.ofNullable(v.getUsedCount()).orElse(0) + 1);
                    voucherRepo.save(v);
                }
            }
        }

        // 3) Giờ vàng (lấy khuyến mãi hợp lệ đầu tiên trong khung giờ & ngày)
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalTime now = LocalTime.now(VN_ZONE);
        DayOfWeek dow = today.getDayOfWeek();

        Optional<Promotion> promoOpt = promoRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> isDayMatched(p, dow))
                .filter(p -> isTimeInRange(now, p.getStartTime(), p.getEndTime()))
                .findFirst();

        if (promoOpt.isPresent()) {
            Promotion p = promoOpt.get();
            double percent = Optional.ofNullable(p.getDiscountPercent()).orElse(0.0);
            total -= subtotal * (percent / 100.0);
        }

        return Math.max(total, 0d);
    }

    // ---------- helpers ----------

    private boolean isVoucherValid(Voucher v) {
        if (v == null || !Boolean.TRUE.equals(v.getActive())) return false;

        LocalDateTime now = LocalDateTime.now(VN_ZONE);

        boolean withinDate =
                (v.getValidFrom() == null || !now.isBefore(v.getValidFrom())) &&
                (v.getValidTo() == null   || !now.isAfter(v.getValidTo()));

        Integer usageLimit = v.getUsageLimit(); // null hoặc 0 => không giới hạn
        Integer used = Optional.ofNullable(v.getUsedCount()).orElse(0);
        boolean underLimit = (usageLimit == null || usageLimit == 0) || used < usageLimit;

        return withinDate && underLimit;
    }

    /** p.getDaysOfWeek() có thể dạng "MON,TUE,WED" hoặc "MONDAY,..." → chấp cả hai */
    private boolean isDayMatched(Promotion p, DayOfWeek dow) {
        String days = Optional.ofNullable(p.getDaysOfWeek()).orElse("").trim();
        if (days.isEmpty()) return false;

        String shortName = dow.name().substring(0, 3);   // MON/TUE/...
        String longName  = dow.name();                   // MONDAY/...

        return Arrays.stream(days.split(","))
                .map(String::trim)
                .anyMatch(s -> s.equalsIgnoreCase(shortName) || s.equalsIgnoreCase(longName));
    }

    private boolean isTimeInRange(LocalTime now, LocalTime start, LocalTime end) {
        if (start == null || end == null) return false;
        // Khung giờ đơn giản start <= now <= end (không xử lý qua đêm)
        return !now.isBefore(start) && !now.isAfter(end);
    }

    private double safeDouble(Double d) {
        return d == null ? 0d : d;
    }


    // ---------- cho mục đích preview (không lưu voucher) ----------
    public double applyDiscountsFromSubtotal(double subtotal, String voucherCode) {
        DiscountBreakdown d = computeDiscounts(subtotal, voucherCode);
        return Math.max(0d, subtotal - d.voucherDiscount - d.promoDiscount);
    }

    @Getter @Setter
    public static class DiscountBreakdown {
        private double voucherDiscount;
        private double promoDiscount;
        private boolean voucherValid;
        private String voucherMessage;
    }

    public DiscountBreakdown computeDiscounts(double subtotal, String voucherCode) {
        DiscountBreakdown out = new DiscountBreakdown();
        double voucherDisc = 0d, promoDisc = 0d;

        // voucher (preview: KHÔNG tăng usedCount)
        if (voucherCode != null && !voucherCode.isBlank()) {
            Optional<Voucher> opt = voucherRepo.findByCodeAndActiveTrue(voucherCode.trim());
            if (opt.isPresent() && isVoucherValid(opt.get())) {
                Voucher v = opt.get();
                if (v.getDiscountPercent() != null) voucherDisc += subtotal * (v.getDiscountPercent()/100.0);
                if (v.getDiscountAmount()  != null) voucherDisc += v.getDiscountAmount();
                out.setVoucherValid(true);
                out.setVoucherMessage("Áp dụng voucher " + v.getCode());
            } else {
                out.setVoucherValid(false);
                out.setVoucherMessage("Mã voucher không hợp lệ hoặc đã hết hạn");
            }
        }

        // promotion theo khung giờ (áp trên subtotal gốc)
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalTime now = LocalTime.now(VN_ZONE);
        DayOfWeek dow = today.getDayOfWeek();
        Optional<Promotion> promoOpt = promoRepo.findAll().stream()
            .filter(p -> Boolean.TRUE.equals(p.getActive()))
            .filter(p -> isDayMatched(p, dow))
            .filter(p -> isTimeInRange(now, p.getStartTime(), p.getEndTime()))
            .findFirst();

        if (promoOpt.isPresent()) {
            double percent = Optional.ofNullable(promoOpt.get().getDiscountPercent()).orElse(0.0);
            promoDisc += subtotal * (percent/100.0);
        }

        out.setVoucherDiscount(voucherDisc);
        out.setPromoDiscount(promoDisc);
        return out;
    }
}
