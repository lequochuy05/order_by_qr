package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.VoucherRequest;
import com.sacmauquan.qrordering.dto.VoucherValidateResponse;
import com.sacmauquan.qrordering.dto.DiscountResult;
import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.repository.VoucherRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service gộp:
 *  - CRUD + validate voucher cho trang quản trị & menu
 *  - Logic apply/preview khi thanh toán (giữ nguyên hàm cũ của bạn)
 */
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final VoucherRepository voucherRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ================== CRUD & Validate cho API ==================

    public List<Voucher> findAll() {
        List<Voucher> list = voucherRepository.findAllByOrderByIdDesc();
        return (list == null || list.isEmpty()) ? voucherRepository.findAll() : list;
    }

    public Voucher findById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher không tồn tại"));
    }

    public Voucher create(VoucherRequest req) {
        normalize(req);
        validateUpsert(req, null);
        Voucher v = new Voucher();
        applyFields(v, req);
        if (v.getUsedCount() == null) v.setUsedCount(0);
        Voucher saved = voucherRepository.save(v);

        broadcastReload(); 
        return saved;
    }

     public Voucher update(Long id, VoucherRequest req) {
        normalize(req);
        Voucher current = findById(id);
        validateUpsert(req, current);
        applyFields(current, req);
        Voucher saved = voucherRepository.save(current);

        broadcastReload(); // ✅ thêm
        return saved;
    }

    public void delete(Long id) {
        if (!voucherRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher không tồn tại");
        }
        voucherRepository.deleteById(id);

        broadcastReload(); // ✅ thêm
    }

    public void increaseUsedCount(Long id) {
        Voucher v = findById(id);
        int used = Optional.ofNullable(v.getUsedCount()).orElse(0);
        v.setUsedCount(used + 1);
        voucherRepository.save(v);

        broadcastReload(); // ✅ thêm
    }

    private void broadcastReload() {
        try {
            messagingTemplate.convertAndSend("/topic/vouchers", "reload");
            log.info("[WS] broadcast /topic/vouchers -> reload");
        } catch (Exception e) {
            log.warn("Không thể broadcast /topic/vouchers", e);
        }
    }

    /** Validate code cho frontend: trả trạng thái + số tiền giảm quy đổi từ % nếu có total */
    public VoucherValidateResponse validateCode(String code, Double orderTotal) {
        if (code == null || code.isBlank()) {
            return new VoucherValidateResponse(null, "NOT_FOUND", 0.0, null, false);
        }
        Voucher v = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElse(null);
        if (v == null) {
            return new VoucherValidateResponse(code.trim(), "NOT_FOUND", 0.0, null, false);
        }

        String status = statusOf(v);
        boolean applicable = "ACTIVE".equals(status);

        double moneyOff = 0.0;
        Double percent = null;
        if (applicable) {
            if (v.getDiscountAmount() != null && v.getDiscountAmount() > 0) {
                moneyOff = v.getDiscountAmount();
            } else if (v.getDiscountPercent() != null && v.getDiscountPercent() > 0) {
                percent = v.getDiscountPercent();
                if (orderTotal != null && orderTotal > 0) {
                    moneyOff = Math.floor(orderTotal * (percent / 100.0));
                }
            }
        }
        return new VoucherValidateResponse(v.getCode(), status, moneyOff, percent, applicable);
    }

    /** Sau khi thanh toán thành công, tăng used_count */
    public void increaseUsedCount(Long id) {
        Voucher v = findById(id);
        int used = Optional.ofNullable(v.getUsedCount()).orElse(0);
        v.setUsedCount(used + 1);
        voucherRepository.save(v);
    }

    // ================== Logic hỗ trợ ==================

    private void normalize(VoucherRequest r) {
        if (r.getCode() != null) r.setCode(r.getCode().trim());
        if (r.getActive() == null) r.setActive(Boolean.TRUE);
        if (r.getUsageLimit() != null && r.getUsageLimit() < 0) r.setUsageLimit(0);
    }

    private void validateUpsert(VoucherRequest r, Voucher current) {
        if (r.getCode() == null || r.getCode().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher không được rỗng");

        boolean codeTaken = voucherRepository.existsByCodeIgnoreCase(r.getCode());
        if (codeTaken && (current == null || !r.getCode().equalsIgnoreCase(current.getCode())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã voucher đã tồn tại");

        boolean hasAmt = r.getDiscountAmount() != null && r.getDiscountAmount() > 0;
        boolean hasPct = r.getDiscountPercent() != null && r.getDiscountPercent() > 0;
        if (!hasAmt && !hasPct)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải có giảm theo tiền hoặc theo %");

        if (hasPct && (r.getDiscountPercent() < 0 || r.getDiscountPercent() > 100))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountPercent phải trong [0..100]");

        if (r.getValidFrom() != null && r.getValidTo() != null && r.getValidFrom().isAfter(r.getValidTo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`validFrom` phải <= `validTo`");
    }

    private void applyFields(Voucher v, VoucherRequest r) {
        v.setCode(r.getCode());
        v.setDiscountAmount(r.getDiscountAmount());
        v.setDiscountPercent(r.getDiscountPercent());
        v.setActive(r.getActive());
        v.setUsageLimit(r.getUsageLimit());
        if (v.getUsedCount() == null) v.setUsedCount(0);
        v.setValidFrom(r.getValidFrom());
        v.setValidTo(r.getValidTo());
    }

    /** ACTIVE | INACTIVE | EXPIRED | EXHAUSTED */
    public String statusOf(Voucher v) {
        LocalDateTime now = LocalDateTime.now();
        boolean inDate = (v.getValidFrom() == null || !now.isBefore(v.getValidFrom()))
                && (v.getValidTo() == null || !now.isAfter(v.getValidTo()));
        boolean underLimit = (v.getUsageLimit() == null || v.getUsageLimit() == 0)
                || (Optional.ofNullable(v.getUsedCount()).orElse(0) < v.getUsageLimit());
        boolean on = Boolean.TRUE.equals(v.getActive());

        if (on && inDate && underLimit) return "ACTIVE";
        if (!on) return "INACTIVE";
        if (!inDate) return "EXPIRED";
        return "EXHAUSTED";
    }

    // ================== Các hàm bạn đã có (giữ nguyên) ==================

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

    /** Dùng để kiểm tra hiển thị ở màn preview (giữ nguyên). */
    public VoucherResult validateVoucher(String code, double subtotal) {
        if (code == null || code.isBlank()) {
            return new VoucherResult(false, "Không có mã giảm giá", 0d);
        }

        Optional<Voucher> opt =
                voucherRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim())
                        .or(() -> voucherRepository.findByCodeAndActiveTrue(code.trim()));

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
