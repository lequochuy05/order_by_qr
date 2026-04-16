package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.VoucherRequest;
import com.sacmauquan.qrordering.dto.VoucherValidateResponse;
import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class DiscountController {
    private final DiscountService service;

    @GetMapping
    public List<Voucher> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Voucher get(@PathVariable Long id) {
        return service.findById(Objects.requireNonNull(id));
    }

    @PostMapping
    public ResponseEntity<Voucher> create(@RequestBody VoucherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    public Voucher update(@PathVariable Long id, @RequestBody VoucherRequest req) {
        return service.update(Objects.requireNonNull(id), req);
    }

    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }

    // Kiểm tra mã + tính tiền giảm theo tổng đơn
    @GetMapping("/validate")
    public VoucherValidateResponse validate(
            @RequestParam String code,
            @RequestParam(required = false) Double total) {
        return service.validateCode(code, total);
    }

    // Tăng used_count sau khi thanh toán thành công
    public ResponseEntity<Void> markUsed(@PathVariable Long id) {
        service.increaseUsedCount(Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }
}
