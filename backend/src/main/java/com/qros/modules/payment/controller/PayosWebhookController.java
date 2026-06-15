package com.qros.modules.payment.controller;

import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.gateway.PayosGateway;
import com.qros.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PayosWebhookController {

    private final PayosGateway payosGateway;
    private final PaymentService paymentService;

    @PostMapping("/payos")
    public ResponseEntity<Map<String, Object>> handlePayosWebhook(@RequestBody Webhook webhook) {
        try {
            PaymentWebhookResult result = payosGateway.verifyWebhook(webhook);
            paymentService.confirmPaymentFromWebhook(result);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (com.qros.shared.exception.BusinessException e) {
            log.error("[PayOS Webhook] Business error processing webhook payload", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[PayOS Webhook] System error processing webhook payload", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}