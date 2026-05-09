package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.dto.PayosCreateResponse;
import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.repository.OrderRepository;
import com.sacmauquan.qrordering.repository.PaymentTransactionRepository;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import com.sacmauquan.qrordering.service.PayosService;
import com.sacmauquan.qrordering.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.math.BigDecimal;

/**
 * PayosServiceImpl - Quản lý quy trình thanh toán qua cổng PayOS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayosServiceImpl implements PayosService {

    private final PayOS payOS;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final DiningTableRepository tableRepository;
    private final NotificationService notificationService;

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public PayosCreateResponse createPaymentLink(@NonNull PayosCreateRequest request) {
        Order order = orderRepository.findById(Objects.requireNonNull(request.getOrderId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng: " + request.getOrderId()));

        // --- KIỂM TRA TRÙNG LẶP (IDEMPOTENCY) ---
        Optional<PaymentTransaction> existing = transactionRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(),
                        PaymentTransaction.TransactionStatus.PENDING);

        if (existing.isPresent()) {
            PaymentTransaction oldTx = existing.get();
            // Giao dịch cũ chưa quá 20 phút và khớp số tiền -> Trả về luôn link cũ
            boolean isExpired = oldTx.getCreatedAt().plusMinutes(20).isBefore(LocalDateTime.now());
            if (!isExpired && oldTx.getAmount().compareTo(request.getAmount()) == 0 && oldTx.getQrCode() != null) {
                log.info("[PayOS Idempotency] Reusing pending transaction {} for order {}", oldTx.getId(),
                        order.getId());
                return convertToCreateResponse(oldTx);
            }
            log.info("[PayOS Idempotency] Existing tx invalid (expired or amount mismatch). Creating new one.");
        }

        // 1. Tạo bản ghi Transaction ở trạng thái PENDING
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(request.getAmount())
                .status(PaymentTransaction.TransactionStatus.PENDING)
                .paymentMethod(PaymentTransaction.PaymentMethod.PAYOS)
                .build();

        transaction = transactionRepository.save(Objects.requireNonNull(transaction));

        // 2. Gọi SDK PayOS tạo Link thanh toán
        try {
            String returnUrl = frontendUrl + "/admin/table-manager";
            long expiredAt = (System.currentTimeMillis() / 1000) + (20 * 60); // 20 phút

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(transaction.getId()) // Sử dụng ID giao dịch làm mã đơn hàng
                    .amount(request.getAmount().longValue())
                    .description("Thanh toan don #" + order.getId())
                    .returnUrl(returnUrl)
                    .cancelUrl(returnUrl)
                    .expiredAt(expiredAt)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);

            // 3. Cập nhật thông tin QR vào Database
            transaction.setCheckoutUrl(response.getCheckoutUrl());
            transaction.setQrCode(response.getQrCode());
            transaction.setPayosReference(response.getPaymentLinkId());
            transactionRepository.save(transaction);

            return convertToCreateResponse(transaction);

        } catch (Exception e) {
            log.error("Lỗi khi tạo PayOS link cho đơn hàng {}: {}", order.getId(), e.getMessage());
            transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể kết nối tới PayOS: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancelPaymentLink(@NonNull Long transactionId, String reason) {
        PaymentTransaction transaction = transactionRepository.findWithOrderById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Giao dịch không tồn tại"));

        if (transaction.getStatus() != PaymentTransaction.TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy giao dịch đang chờ thanh toán.");
        }

        try {
            payOS.paymentRequests().cancel(transaction.getId(), reason);

            transaction.setStatus(PaymentTransaction.TransactionStatus.CANCELLED);
            transaction.setCancelReason(reason);
            transactionRepository.save(transaction);

            log.info("Đã hủy giao dịch PayOS ID: {}", transactionId);
        } catch (Exception e) {
            log.error("Lỗi khi hủy giao dịch PayOS {}: {}", transactionId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi hủy giao dịch PayOS: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentTransaction syncPaymentStatus(@NonNull Long transactionId) {
        PaymentTransaction transaction = transactionRepository.findWithOrderById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Giao dịch không tồn tại"));

        if (transaction.getStatus() == PaymentTransaction.TransactionStatus.PENDING) {
            try {
                PaymentLink data = payOS.paymentRequests().get(transaction.getId());
                String remoteStatus = data.getStatus().toString();

                log.info("[PayOS Sync] Tx: {}, PayOS Status: {}", transactionId, remoteStatus);

                if ("PAID".equalsIgnoreCase(remoteStatus)) {
                    handleSuccessfulPayment(transaction);
                } else if ("CANCELLED".equalsIgnoreCase(remoteStatus) || "EXPIRED".equalsIgnoreCase(remoteStatus)) {
                    transaction.setStatus(PaymentTransaction.TransactionStatus.CANCELLED);
                    transactionRepository.save(transaction);
                }
            } catch (Exception e) {
                log.error("Lỗi khi đồng bộ trạng thái PayOS {}: {}", transactionId, e.getMessage());
            }
        }
        return transaction;
    }

    @Override
    @Transactional
    public void processWebhook(Webhook webhook) {
        if ("00".equals(webhook.getCode()) && webhook.getData() != null
                && webhook.getData().getOrderCode() != null
                && webhook.getData().getOrderCode() == 0) {
            log.info("[PayOS Webhook] Received confirm webhook URL test.");
            return;
        }

        WebhookData webhookData;
        try {
            webhookData = payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.error("[PayOS Webhook] Signature verification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Xác thực chữ ký thất bại");
        }

        long transactionId = webhookData.getOrderCode();
        log.info("[PayOS Webhook] Verified! transactionId={}, amount={}", transactionId, webhookData.getAmount());

        PaymentTransaction transaction = transactionRepository.findWithOrderById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy giao dịch: " + transactionId));
        if (transaction.getStatus() == PaymentTransaction.TransactionStatus.PAID) {
            log.info("[PayOS Webhook] Transaction {} already PAID, skipping.", transactionId);
            return;
        }

        transaction.setPayosReference(webhookData.getPaymentLinkId());
        handleSuccessfulPayment(transaction);
    }

    /**
     * Xử lý các tác vụ khi thanh toán thành công
     */
    private void handleSuccessfulPayment(PaymentTransaction transaction) {
        transaction.setStatus(PaymentTransaction.TransactionStatus.PAID);
        transactionRepository.save(transaction);

        Order order = transaction.getOrder();

        // Kiểm tra tổng tiền đã thanh toán vs tổng đơn
        BigDecimal totalPaid = transactionRepository.sumPaidAmountByOrderId(order.getId());
        BigDecimal totalBill = order.getTotalAmount();

        log.info("[Payment Progress] Order #{}: Paid {} / Total {}", order.getId(), totalPaid, totalBill);

        if (totalPaid.compareTo(totalBill) >= 0) {
            if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setPaymentMethod(Order.PaymentMethod.PAYOS);
                order.setPaymentTime(LocalDateTime.now());
                order.setStatus(Order.OrderStatus.COMPLETED);
                orderRepository.save(order);

                // Giải phóng bàn
                DiningTable table = order.getTable();
                if (table != null) {
                    table.setStatus(DiningTable.TableStatus.AVAILABLE);
                    tableRepository.save(table);
                    notificationService.notifyTableChange();
                }

                log.info("Thanh toán HOÀN TẤT đơn hàng #{} qua PayOS", order.getId());
            }
        } else {
            log.info("Thanh toán MỘT PHẦN đơn hàng #{} (Chờ thanh toán đủ)", order.getId());
        }

        // Luôn gửi thông báo WebSocket cho mỗi giao dịch thành công
        notificationService.notifyPaymentSuccess(order.getId(), transaction.getId());
    }

    private PayosCreateResponse convertToCreateResponse(PaymentTransaction tx) {
        return PayosCreateResponse.builder()
                .transactionId(tx.getId())
                .checkoutUrl(tx.getCheckoutUrl())
                .qrCode(tx.getQrCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
