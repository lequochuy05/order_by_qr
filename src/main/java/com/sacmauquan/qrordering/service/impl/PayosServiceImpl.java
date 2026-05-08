package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.dto.PayosCreateResponse;
import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.repository.OrderRepository;
import com.sacmauquan.qrordering.repository.PaymentTransactionRepository;
import com.sacmauquan.qrordering.service.PayosService;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import com.sacmauquan.qrordering.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.PaymentLink;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
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
    public PayosCreateResponse createPaymentLink(PayosCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderId()));

        // --- IDEMPOTENCY LOGIC ---
        // 1. Kiểm tra xem đã có giao dịch PENDING nào cho Order này với cùng số tiền
        // chưa
        Optional<PaymentTransaction> existing = transactionRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentTransaction.PENDING);

        if (existing.isPresent()) {
            PaymentTransaction oldTx = existing.get();
            // Kiểm tra xem giao dịch cũ đã quá 20 phút chưa
            boolean isExpired = oldTx.getCreatedAt().plusMinutes(20).isBefore(LocalDateTime.now());

            // Nếu chưa hết hạn, số tiền khớp và đã có link, trả về luôn mã cũ
            if (!isExpired && oldTx.getAmount().compareTo(request.getAmount()) == 0 && oldTx.getQrCode() != null) {
                log.info("[Idempotency] Reusing existing pending transaction {} for order {}", 
                        oldTx.getId(), order.getId());
                return PayosCreateResponse.builder()
                        .transactionId(oldTx.getId())
                        .checkoutUrl(oldTx.getCheckoutUrl())
                        .qrCode(oldTx.getQrCode())
                        .createdAt(oldTx.getCreatedAt())
                        .build();
            }
            log.info("[Idempotency] Existing tx expired, amount mismatch, or no link. Creating new one.");
        }

        // 2. Tạo Transaction PENDING mới
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(request.getAmount())
                .status(PaymentTransaction.PENDING)
                .paymentMethod("PAYOS")
                .build();

        // Lưu để lấy ID tự tăng làm orderCode cho PayOS
        transaction = transactionRepository.save(transaction);

        // 2. Gọi SDK PayOS
        try {
            // URL khi khách hàng quét QR và ấn Hủy/Thành công trên đt
            String returnUrl = frontendUrl + "/admin/table-manager";
            String cancelUrl = frontendUrl + "/admin/table-manager";

            // Set thời gian hết hạn là 20 phút kể từ lúc tạo
            long expiredAt = (System.currentTimeMillis() / 1000) + (20 * 60);

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(transaction.getId()) // Bắt buộc phải là ID độc nhất
                    .amount(request.getAmount().longValue())
                    .description("Thanh toan don " + order.getId())
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .expiredAt(expiredAt)
                    .build();

            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);

            // 3. Lưu thông tin QR trả về
            transaction.setCheckoutUrl(data.getCheckoutUrl());
            transaction.setQrCode(data.getQrCode());
            transaction.setPayosReference(data.getPaymentLinkId());
            transactionRepository.save(transaction);

            return PayosCreateResponse.builder()
                    .transactionId(transaction.getId())
                    .checkoutUrl(data.getCheckoutUrl())
                    .qrCode(data.getQrCode())
                    .createdAt(transaction.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi tạo PayOS link cho order {}: [{}] {}", order.getId(), e.getClass().getSimpleName(),
                    e.getMessage(), e);
            transaction.setStatus(PaymentTransaction.FAILED);
            transactionRepository.save(transaction);
            throw new RuntimeException("Không thể kết nối tới PayOS: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancelPaymentLink(Long transactionId, String reason) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!PaymentTransaction.PENDING.equals(transaction.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy giao dịch đang chờ thanh toán.");
        }

        try {
            payOS.paymentRequests().cancel(transaction.getId(), reason);

            transaction.setStatus(PaymentTransaction.CANCELLED);
            transaction.setCancelReason(reason);
            transactionRepository.save(transaction);

            log.info("Đã hủy giao dịch PayOS: {}", transactionId);
        } catch (Exception e) {
            log.error("Lỗi khi hủy PayOS link {}: ", transactionId, e);
            throw new RuntimeException("Lỗi hủy giao dịch PayOS: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentTransaction syncPaymentStatus(Long transactionId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (PaymentTransaction.PENDING.equals(transaction.getStatus())) {
            try {
                PaymentLink data = payOS.paymentRequests().get(transaction.getId());
                String status = data.getStatus().toString();
                log.info("[PayOS Polling] Transaction ID: {}, Status from PayOS: {}, AmountPaid: {}",
                        transactionId, status, data.getAmountPaid());

                if ("PAID".equals(status)) {
                    transaction.setStatus(PaymentTransaction.PAID);
                    transactionRepository.save(transaction);

                    // Fallback logic nếu Webhook không chạy
                    Order order = transaction.getOrder();
                    if (!"PAID".equals(order.getPaymentStatus())) {
                        order.setPaymentStatus("PAID");
                        order.setPaymentMethod("PAYOS");
                        order.setPaymentTime(LocalDateTime.now());
                        order.setStatus("COMPLETED");
                        orderRepository.save(order);

                        if (order.getTable() != null) {
                            com.sacmauquan.qrordering.model.DiningTable table = order.getTable();
                            table.setStatus(com.sacmauquan.qrordering.model.DiningTable.AVAILABLE);
                            tableRepository.save(table);
                            notificationService.notifyTableChange();
                        }
                        notificationService.notifyPaymentSuccess(order.getId(), transaction.getId());
                    }
                } else if ("CANCELLED".equals(status) || "EXPIRED".equals(status)) {
                    transaction.setStatus(PaymentTransaction.CANCELLED);
                    transactionRepository.save(transaction);
                }
            } catch (Exception e) {
                log.error("Lỗi khi đồng bộ trạng thái PayOS {}: ", transactionId, e);
            }
        }
        return transaction;
    }
}
