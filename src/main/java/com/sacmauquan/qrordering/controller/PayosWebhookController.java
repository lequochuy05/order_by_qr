package com.sacmauquan.qrordering.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.repository.OrderRepository;
import com.sacmauquan.qrordering.repository.PaymentTransactionRepository;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import com.sacmauquan.qrordering.service.NotificationService;
import com.sacmauquan.qrordering.model.DiningTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PayosWebhookController {

    private final PayOS payOS;
    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final DiningTableRepository tableRepository;
    private final NotificationService notificationService;

    @PostMapping("/payos")
    @Transactional
    public ResponseEntity<Map<String, Object>> handlePayosWebhook(@RequestBody Webhook webhook) {
        log.info("=== [PayOS Webhook] Received payload ===");

        try {
            // PayOS gửi webhook xác nhận (confirm URL) với code "00" và orderCode = 0
            if ("00".equals(webhook.getCode()) && webhook.getData() != null && webhook.getData().getOrderCode() != null
                    && webhook.getData().getOrderCode() == 0) {
                log.info("[PayOS Webhook] Confirm webhook URL test - returning OK");
                return ResponseEntity.ok(Map.of("success", true));
            }

            // 2. Verify signature bằng PayOS SDK
            WebhookData webhookData = payOS.webhooks().verify(webhook);

            long orderCode = webhookData.getOrderCode();
            log.info("[PayOS Webhook] Verified! orderCode={}, amount={}",
                    orderCode, webhookData.getAmount());

            // 3. Tìm PaymentTransaction theo ID (= orderCode)
            PaymentTransaction transaction = transactionRepository.findById(orderCode)
                    .orElse(null);

            if (transaction == null) {
                log.warn("[PayOS Webhook] Transaction not found for orderCode: {}", orderCode);
                return ResponseEntity.ok(Map.of("success", true));
            }

            // 4. Nếu transaction đã PAID rồi thì bỏ qua (idempotent)
            if (PaymentTransaction.PAID.equals(transaction.getStatus())) {
                log.info("[PayOS Webhook] Transaction {} already PAID, skipping", orderCode);
                return ResponseEntity.ok(Map.of("success", true));
            }

            // 5. Cập nhật transaction -> PAID
            transaction.setStatus(PaymentTransaction.PAID);
            transaction.setPayosReference(webhookData.getPaymentLinkId());
            transactionRepository.save(transaction);
            log.info("[PayOS Webhook] Transaction {} -> PAID", orderCode);

            // 6. Kiểm tra tổng tiền đã thanh toán vs tổng bill (hỗ trợ tách bill)
            Order order = transaction.getOrder();
            BigDecimal totalPaid = transactionRepository.sumPaidAmountByOrderId(order.getId());
            BigDecimal totalBill = BigDecimal.valueOf(order.getTotalAmount());

            if (totalPaid.compareTo(totalBill) >= 0) {
                order.setPaymentStatus("PAID");
                order.setPaymentMethod("PAYOS");
                order.setPaymentTime(LocalDateTime.now());
                order.setStatus("COMPLETED");
                orderRepository.save(order);

                // Giải phóng bàn
                if (order.getTable() != null) {
                    DiningTable table = order.getTable();
                    table.setStatus(DiningTable.AVAILABLE);
                    tableRepository.save(table);
                    notificationService.notifyTableChange();
                }
            } else {
                log.info("[PayOS Webhook] Order {} partially paid: {}/{}",
                        order.getId(), totalPaid, totalBill);
            }

            // 7. Bắn WebSocket thông báo realtime cho Admin
            notificationService.notifyPaymentSuccess(order.getId(), transaction.getId());

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            log.error("[PayOS Webhook] Error processing webhook: ", e);
            // Vẫn trả 200 để PayOS không retry liên tục
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
