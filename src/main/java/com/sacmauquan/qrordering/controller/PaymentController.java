package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.dto.PayosCreateResponse;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.service.PayosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PaymentController - Quản lý tích hợp cổng thanh toán PayOS.
 */
@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
public class PaymentController {

    private final PayosService payosService;

    /**
     * Khởi tạo liên kết thanh toán PayOS (QR Code thanh toán)
     */
    @PostMapping("/create")
    public ApiResponse<PayosCreateResponse> createPaymentLink(@Valid @RequestBody @NonNull PayosCreateRequest request) {
        return ApiResponse.success("Khởi tạo thanh toán thành công", payosService.createPaymentLink(request));
    }

    /**
     * Hủy liên kết thanh toán và cập nhật lý do hủy
     */
    @PostMapping("/{transactionId}/cancel")
    public ApiResponse<Void> cancelPaymentLink(
            @PathVariable @NonNull Long transactionId,
            @RequestBody @NonNull Map<String, String> body) {

        String reason = body.getOrDefault("reason", "Khách đổi hình thức thanh toán");
        payosService.cancelPaymentLink(transactionId, reason);
        return ApiResponse.success("Đã hủy giao dịch thanh toán thành công", null);
    }

    /**
     * Đồng bộ trạng thái giao dịch thực tế từ PayOS về hệ thống.
     */
    @GetMapping("/{transactionId}/status")
    public ApiResponse<PaymentTransaction> syncPaymentStatus(@PathVariable @NonNull Long transactionId) {
        return ApiResponse.success(payosService.syncPaymentStatus(transactionId));
    }
}
