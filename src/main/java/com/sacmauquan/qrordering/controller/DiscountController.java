package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.VoucherRequest;
import com.sacmauquan.qrordering.dto.VoucherValidateResponse;
import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class DiscountController {
    private final DiscountService service;

    @GetMapping
    public List<Voucher> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public Voucher get(@PathVariable Long id){ return service.findById(id); }

    @PostMapping
    public ResponseEntity<Voucher> create(@RequestBody VoucherRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public Voucher update(@PathVariable Long id, @RequestBody VoucherRequest req){
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Kiểm tra mã + tính tiền giảm theo tổng đơn
    @GetMapping("/validate")
    public VoucherValidateResponse validate(
            @RequestParam String code,
            @RequestParam(required = false) Double total
    ){
        return service.validateCode(code, total);
    }

    // Tăng used_count sau khi thanh toán thành công
    @PostMapping("/{id}/use")
    public ResponseEntity<Void> markUsed(@PathVariable Long id){
        service.increaseUsedCount(id);
        return ResponseEntity.noContent().build();
    }
}
