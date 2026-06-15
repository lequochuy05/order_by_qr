package com.qros.modules.promotion.controller;

import com.qros.modules.promotion.dto.request.VoucherRequest;
import com.qros.modules.promotion.dto.response.VoucherResponse;
import com.qros.modules.promotion.dto.response.VoucherValidateResponse;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.promotion.service.VoucherService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
@Validated
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final VoucherCheckoutService voucherCheckoutService;

    @GetMapping
    public ApiResponse<Page<VoucherResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ApiResponse.success(voucherService.searchForManagement(q, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<VoucherResponse> get(@PathVariable Long id) {
        return ApiResponse.success(voucherService.findById(id));
    }

    @PostMapping
    public ApiResponse<VoucherResponse> create(@Valid @RequestBody VoucherRequest request) {
        return ApiResponse.success(
                "Voucher created successfully",
                voucherService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<VoucherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequest request) {
        return ApiResponse.success(
                "Voucher updated successfully",
                voucherService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        voucherService.delete(id);
        return ApiResponse.success("Voucher deleted successfully", null);
    }

    @GetMapping("/validate")
    public ApiResponse<VoucherValidateResponse> validate(
            @RequestParam @NotBlank(message = "Voucher code cannot be empty") String code,

            @RequestParam(defaultValue = "0") @DecimalMin(value = "0.00", message = "Subtotal cannot be negative") BigDecimal subtotal) {
        return ApiResponse.success(voucherCheckoutService.validateCode(code, subtotal));
    }
}
