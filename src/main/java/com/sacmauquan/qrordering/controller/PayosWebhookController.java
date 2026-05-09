package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.service.PayosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;

import java.util.Map;

/**
 * PayosWebhookController - Tiếp nhận thông báo thanh toán tự động từ PayOS
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PayosWebhookController {

    private final PayosService payosService;

    /**
     * Tiếp nhận Webhook xác nhận thanh toán từ PayOS
     */
    @PostMapping("/payos")
    public ResponseEntity<Map<String, Object>> handlePayosWebhook(@RequestBody Webhook webhook) {
        log.info("=== [PayOS Webhook] Received payload ===");

        try {
            payosService.processWebhook(webhook);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("[PayOS Webhook] Error processing webhook payload: ", e);
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
