package com.qros.modules.payment.service.impl;

import com.qros.modules.payment.dto.PayosCreateRequest;
import com.qros.modules.payment.dto.PayosCreateResponse;
import com.qros.modules.analytics.service.ReportingSummaryService;
import com.qros.modules.order.model.Order;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderAuditService;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.user.repository.UserRepository;
import com.qros.modules.payment.service.PayosService;
import com.qros.shared.util.AppTime;
import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.transaction.TransactionSideEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
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
import com.qros.modules.promotion.service.DiscountService;
import com.qros.modules.promotion.service.OrderDiscountService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

import java.util.Objects;
import java.util.Optional;
import java.math.BigDecimal;

/**
 * PayosServiceImpl - Implementation of PayosService for handling PayOS gateway
 * operations.
 * Manages the full payment lifecycle including link generation, webhook
 * verification, and state synchronization.
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
    private final TransactionSideEffectService sideEffects;
    private final DiscountService discountService;
    private final OrderPricingService orderPricingService;
    private final OrderAuditService orderAuditService;
    private final OrderDiscountService orderDiscountService;
    private final ReportingSummaryService reportingSummaryService;
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;

    private Counter paymentsCompletedCounter;

    @PostConstruct
    public void initCounters() {
        paymentsCompletedCounter = Counter.builder("payments.completed")
                .description("Total number of successful PayOS payment completions")
                .register(meterRegistry);
    }

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    /**
     * Creates a payment link via PayOS. Includes idempotency check to reuse
     * existing pending links.
     * 
     * @param request Payment creation parameters
     * @return Created payment details
     */
    @Override
    @Transactional
    public PayosCreateResponse createPaymentLink(@NonNull PayosCreateRequest request) {
        Order order = orderRepository.findById(Objects.requireNonNull(request.getOrderId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + request.getOrderId()));
        BigDecimal payableAmount = currentFinalAmount(order);
        if (payableAmount == null || payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has no payable amount");
        }
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID || order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is already paid");
        }

        String idempotencyKey = normalizeIdempotencyKey(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            Optional<PaymentTransaction> existingByKey = transactionRepository.findFirstByIdempotencyKey(idempotencyKey);
            if (existingByKey.isPresent()) {
                return reuseIdempotentTransaction(existingByKey.get(), order, payableAmount);
            }
        }

        // Apply voucher if present in request
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            BigDecimal subtotal = currentSubtotalAmount(order);
            var vr = discountService.validateCode(request.getVoucherCode(), subtotal);
            if (vr.applicable()) {
                order.setVoucherCode(vr.code());
                orderPricingService.setOrderMoney(order, subtotal, vr.discountValue());
                orderRepository.save(order);
                payableAmount = currentFinalAmount(order);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher invalid: " + vr.status());
            }
        }

        // IDEMPOTENCY CHECK
        // Prevents generating multiple active PayOS links for the same order and amount
        Optional<PaymentTransaction> existing = transactionRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(),
                        PaymentTransaction.TransactionStatus.PENDING);

        if (existing.isPresent()) {
            PaymentTransaction oldTx = existing.get();
            boolean isExpired = oldTx.getCreatedAt().plusMinutes(20).isBefore(AppTime.now());
            if (!isExpired && oldTx.getAmount().compareTo(payableAmount) == 0 && oldTx.getQrCode() != null) {
                log.info("[PayOS Idempotency] Reusing pending transaction {} for order {}", oldTx.getId(),
                        order.getId());
                return convertToCreateResponse(oldTx);
            }
            log.info("[PayOS Idempotency] Existing pending transaction is invalid or mismatched. Creating new one.");
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(payableAmount)
                .status(PaymentTransaction.TransactionStatus.PENDING)
                .paymentMethod(PaymentTransaction.PaymentMethod.PAYOS)
                .businessDate(order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today())
                .idempotencyKey(idempotencyKey)
                .createdBy(request.getCreatedById() != null ? userRepository.findById(request.getCreatedById()).orElse(null) : null)
                .build();

        transaction = transactionRepository.save(Objects.requireNonNull(transaction));

        try {
            String returnUrl = frontendUrl + "/admin/table-manager";
            long expiredAt = (System.currentTimeMillis() / 1000) + (20 * 60); // Link valid for 20 minutes

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(transaction.getId()) // PayOS uses our Transaction ID as their Order Code
                    .amount(payableAmount.longValue())
                    .description("Payment for Order #" + order.getId())
                    .returnUrl(returnUrl)
                    .cancelUrl(returnUrl)
                    .expiredAt(expiredAt)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);

            transaction.setCheckoutUrl(response.getCheckoutUrl());
            transaction.setQrCode(response.getQrCode());
            transaction.setExternalReference(response.getPaymentLinkId());
            transaction.setProviderPayload(providerPayload("PAYOS", "CREATE_LINK", response.getPaymentLinkId()));
            transactionRepository.save(transaction);
            PaymentTransaction createdTransaction = transaction;
            sideEffects.afterRollback(() -> {
                try {
                    payOS.paymentRequests().cancel(createdTransaction.getId(),
                            "Local transaction rolled back after PayOS link creation");
                } catch (Exception e) {
                    log.error("Failed to cancel rolled back PayOS link {}: {}", createdTransaction.getId(),
                            e.getMessage());
                }
            }, "cancel rolled back PayOS link " + createdTransaction.getId());

            return convertToCreateResponse(transaction);

        } catch (Exception e) {
            log.error("Failed to generate PayOS link for Order {}: {}", order.getId(), e.getMessage());
            transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transaction.setProviderPayload(providerPayload("PAYOS", "CREATE_LINK_FAILED", null));
            transactionRepository.save(transaction);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "PayOS Gateway error: " + e.getMessage());
        }
    }

    /**
     * Cancels a pending PayOS payment link.
     */
    @Override
    @Transactional
    public void cancelPaymentLink(@NonNull Long transactionId, String reason) {
        PaymentTransaction transaction = transactionRepository.findWithOrderById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (transaction.getStatus() != PaymentTransaction.TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING transactions can be cancelled");
        }

        try {
            payOS.paymentRequests().cancel(transaction.getId(), reason);

            transaction.setStatus(PaymentTransaction.TransactionStatus.CANCELLED);
            transaction.setFailureReason(reason);
            transaction.setProviderPayload(providerPayload("PAYOS", "CANCEL_LINK", transaction.getExternalReference()));
            transactionRepository.save(transaction);

            log.info("PayOS payment link cancelled for Transaction ID: {}", transactionId);
        } catch (Exception e) {
            log.error("Failed to cancel PayOS link {}: {}", transactionId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "PayOS Cancellation error: " + e.getMessage());
        }
    }

    /**
     * Actively synchronizes local transaction status with PayOS server.
     */
    @Override
    @Transactional
    @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance",
            "stats_dish_trend", "stats_dashboard", "order_by_id", "order_stats" }, allEntries = true)
    public PaymentTransaction syncPaymentStatus(@NonNull Long transactionId) {
        PaymentTransaction transaction = transactionRepository.findWithOrderById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (transaction.getStatus() == PaymentTransaction.TransactionStatus.PENDING) {
            try {
                PaymentLink data = payOS.paymentRequests().get(transaction.getId());
                String remoteStatus = data.getStatus().toString();

                log.info("[PayOS Sync] Transaction: {}, Remote Status: {}", transactionId, remoteStatus);

                if ("PAID".equalsIgnoreCase(remoteStatus)) {
                    handleSuccessfulPayment(transaction);
                } else if ("CANCELLED".equalsIgnoreCase(remoteStatus) || "EXPIRED".equalsIgnoreCase(remoteStatus)) {
                    transaction.setStatus(PaymentTransaction.TransactionStatus.CANCELLED);
                    transaction.setFailureReason(remoteStatus);
                    transaction.setProviderPayload(providerPayload("PAYOS", remoteStatus.toUpperCase(), transaction.getExternalReference()));
                    transactionRepository.save(transaction);
                }
            } catch (Exception e) {
                log.error("PayOS status sync failed for {}: {}", transactionId, e.getMessage());
            }
        }
        return transaction;
    }

    /**
     * Validates and processes incoming webhook notifications from PayOS.
     */
    @Override
    @Transactional
    @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance",
            "stats_dish_trend", "stats_dashboard", "order_by_id", "order_stats" }, allEntries = true)
    public void processWebhook(Webhook webhook) {
        // Handle confirm URL test from PayOS dashboard
        if ("00".equals(webhook.getCode()) && webhook.getData() != null
                && webhook.getData().getOrderCode() != null
                && webhook.getData().getOrderCode() == 0) {
            log.info("[PayOS Webhook] confirm_url validation received.");
            return;
        }

        WebhookData webhookData;
        try {
            webhookData = payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.error("[PayOS Webhook] Signature verification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }

        long transactionId = webhookData.getOrderCode();
        log.info("[PayOS Webhook] Verified transaction: {} | Amount: {}", transactionId, webhookData.getAmount());

        PaymentTransaction transaction = transactionRepository.findWithOrderById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + transactionId));

        if (transaction.getStatus() == PaymentTransaction.TransactionStatus.PAID) {
            log.info("[PayOS Webhook] Transaction {} already marked as PAID.", transactionId);
            return;
        }
        BigDecimal webhookAmount = BigDecimal.valueOf(webhookData.getAmount());
        if (transaction.getAmount().compareTo(webhookAmount) != 0) {
            log.error("[PayOS Webhook] Amount mismatch for transaction {}. Expected {}, got {}",
                    transactionId, transaction.getAmount(), webhookAmount);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook amount does not match transaction");
        }

        transaction.setExternalReference(webhookData.getPaymentLinkId());
        transaction.setProviderPayload(providerPayload("PAYOS", "WEBHOOK_PAID", webhookData.getPaymentLinkId()));
        handleSuccessfulPayment(transaction);
    }

    /**
     * Core logic for finalizing an order once a successful payment is confirmed.
     */
    private void handleSuccessfulPayment(PaymentTransaction transaction) {
        transaction.setStatus(PaymentTransaction.TransactionStatus.PAID);
        if (transaction.getPaidAt() == null) {
            transaction.setPaidAt(AppTime.now());
        }
        if (transaction.getBusinessDate() == null) {
            transaction.setBusinessDate(transaction.getPaidAt().toLocalDate());
        }
        transactionRepository.save(transaction);

        Order order = transaction.getOrder();

        // Calculate if the sum of all successful transactions covers the order total
        BigDecimal totalPaid = transactionRepository.sumPaidAmountByOrderId(order.getId());
        BigDecimal totalBill = currentFinalAmount(order);

        log.info("[Payment Validation] Order #{}: Total Paid: {} | Total Bill: {}", order.getId(), totalPaid,
                totalBill);

        if (totalPaid.compareTo(totalBill) >= 0) {
            if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                Order.OrderStatus fromStatus = order.getStatus();
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setPaymentMethod(Order.PaymentMethod.PAYOS);
                order.setPaymentTime(AppTime.now());
                order.setPaidBy(transaction.getCreatedBy());
                order.setPaidAmount(totalPaid);
                order.setBusinessDate(order.getPaymentTime().toLocalDate());
                order.setStatus(Order.OrderStatus.COMPLETED);
                orderRepository.save(order);
                orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), transaction.getCreatedBy(),
                        "payos-payment");
                orderDiscountService.recordVoucherSnapshot(order, order.getVoucherCode(), order.getDiscountAmount());
                reportingSummaryService.recordCompletedOrder(order);

                // Increment voucher usage if applied
                if (order.getVoucherCode() != null) {
                    discountService.incrementUsage(order.getVoucherCode());
                }

                // Transition table back to AVAILABLE
                DiningTable table = order.getTable();
                if (table != null) {
                    table.setStatus(DiningTable.TableStatus.AVAILABLE);
                    tableRepository.save(table);
                    notificationService.notifyTableChange();
                }

                log.info("Order #{} successfully completed via full PayOS payment", order.getId());
            }
        } else {
            log.info("Partial payment recorded for Order #{}. Awaiting remaining balance.", order.getId());
        }

        // Notify frontend in real-time
        notificationService.notifyPaymentSuccess(order.getId(), transaction.getId());
        paymentsCompletedCounter.increment();
    }

    private PayosCreateResponse convertToCreateResponse(PaymentTransaction tx) {
        return PayosCreateResponse.builder()
                .transactionId(tx.getId())
                .checkoutUrl(tx.getCheckoutUrl())
                .qrCode(tx.getQrCode())
                .createdAt(tx.getCreatedAt())
                .amount(tx.getAmount())
                .subtotalAmount(tx.getOrder() != null ? tx.getOrder().getSubtotalAmount() : null)
                .discountAmount(tx.getOrder() != null ? tx.getOrder().getDiscountAmount() : null)
                .finalAmount(tx.getAmount())
                .originalTotal(tx.getOrder() != null ? tx.getOrder().getSubtotalAmount() : null)
                .discountVoucher(tx.getOrder() != null ? tx.getOrder().getDiscountAmount() : null)
                .voucherCode(tx.getOrder() != null ? tx.getOrder().getVoucherCode() : null)
                .idempotencyKey(tx.getIdempotencyKey())
                .externalReference(tx.getExternalReference())
                .build();
    }

    private PayosCreateResponse reuseIdempotentTransaction(PaymentTransaction tx, Order order, BigDecimal amount) {
        if (!Objects.equals(tx.getOrder().getId(), order.getId()) || tx.getAmount().compareTo(amount) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency key is already associated with another payment request");
        }
        if (tx.getStatus() == PaymentTransaction.TransactionStatus.FAILED
                || tx.getStatus() == PaymentTransaction.TransactionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency key is already associated with an inactive payment transaction");
        }
        log.info("[PayOS Idempotency] Reusing transaction {} for key {}", tx.getId(), tx.getIdempotencyKey());
        return convertToCreateResponse(tx);
    }

    private BigDecimal currentFinalAmount(Order order) {
        return order.getFinalAmount();
    }

    private BigDecimal currentSubtotalAmount(Order order) {
        return order.getSubtotalAmount();
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim();
    }

    private String providerPayload(String provider, String event, String reference) {
        String safeReference = reference != null ? reference.replace("\"", "") : "";
        return "{\"provider\":\"" + provider + "\",\"event\":\"" + event + "\",\"reference\":\"" + safeReference
                + "\"}";
    }
}
