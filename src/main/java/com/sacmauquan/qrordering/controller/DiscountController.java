package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.VoucherRequest;
import com.sacmauquan.qrordering.dto.VoucherValidateResponse;
import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DiscountController - Quản lý Voucher và mã giảm giá.
 */
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService service;

    /**
     * Danh sách toàn bộ Voucher
     */
    @GetMapping
    public ApiResponse<List<Voucher>> list() {
        return ApiResponse.success(service.findAll());
    }

    /**
     * Lấy chi tiết Voucher theo ID
     */
    @GetMapping("/{id}")
    public ApiResponse<Voucher> get(@PathVariable @NonNull Long id) {
        return ApiResponse.success(service.findById(id));
    }

    /**
     * Tạo mới Voucher (Tự động uppercase mã code trong Service)
     */
    @PostMapping
    public ApiResponse<Voucher> create(@Valid @RequestBody @NonNull VoucherRequest req) {
        return ApiResponse.success("Tạo voucher thành công", service.create(req));
    }

    /**
     * Cập nhật thông tin Voucher
     */
    @PutMapping("/{id}")
    public ApiResponse<Voucher> update(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull VoucherRequest req) {
        return ApiResponse.success("Cập nhật voucher thành công", service.update(id, req));
    }

    /**
     * Xóa Voucher khỏi hệ thống
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        service.delete(id);
        return ApiResponse.success("Xóa voucher thành công", null);
    }

    /**
     * Kiểm tra tính hợp lệ của mã Voucher và tính toán số tiền giảm giá.
     */
    @GetMapping("/validate")
    public ApiResponse<VoucherValidateResponse> validate(
            @RequestParam @NonNull String code,
            @RequestParam(defaultValue = "0") BigDecimal total) {
        return ApiResponse.success(service.validateCode(code, total));
    }
}
