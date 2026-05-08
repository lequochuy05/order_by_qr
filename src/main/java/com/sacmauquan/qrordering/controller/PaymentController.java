package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.dto.PayosCreateResponse;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.service.PayosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
public class PaymentController {

    private final PayosService payosService;

    @PostMapping("/create")
    public ResponseEntity<PayosCreateResponse> createPaymentLink(@Valid @RequestBody PayosCreateRequest request) {
        return ResponseEntity.ok(payosService.createPaymentLink(request));
    }

    @PostMapping("/{transactionId}/cancel")
    public ResponseEntity<Map<String, String>> cancelPaymentLink(
            @PathVariable Long transactionId,
            @RequestBody Map<String, String> body) {

        String reason = body.getOrDefault("reason", "Khách đổi hình thức thanh toán");
        payosService.cancelPaymentLink(transactionId, reason);
        return ResponseEntity.ok(Map.of("message", "Đã hủy giao dịch thành công"));
    }

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<PaymentTransaction> syncPaymentStatus(@PathVariable Long transactionId) {
        return ResponseEntity.ok(payosService.syncPaymentStatus(transactionId));
    }
}
