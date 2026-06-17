package com.qros.modules.payment.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentGatewayStatusResult;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.dto.response.PaymentTransactionResponse;
import com.qros.modules.payment.gateway.PaymentGateway;
import com.qros.modules.payment.gateway.PaymentGatewayResolver;
import com.qros.modules.payment.mapper.PaymentMapper;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int PAYMENT_LINK_EXPIRY_MINUTES = 20;

    private final PaymentTransactionRepository transactionRepository;

    private final PaymentGatewayResolver gatewayResolver;
    private final PaymentMapper paymentMapper;
    private final PaymentCompletionService paymentCompletionService;
    private final OrderSettlementService orderSettlementService;

    private final TransactionSideEffectService sideEffects;

    @Transactional
    public PaymentCreateResponse createPaymentLink(@NonNull PaymentCreateRequest request) {
        PaymentMethod method = request.paymentMethod();

        if (method == PaymentMethod.CASH) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID, "CASH payment must be settled through order payment API");
        }

        Order order = orderSettlementService.prepareForOnlinePayment(
                Objects.requireNonNull(request.orderId()), request.voucherCode());

        PaymentGateway gateway = gatewayResolver.resolve(method);
        BigDecimal payableAmount = currentFinalAmount(order);

        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());

        if (idempotencyKey != null) {
            Optional<PaymentTransaction> existingByKey =
                    transactionRepository.findFirstByIdempotencyKey(idempotencyKey);

            if (existingByKey.isPresent()) {
                return reuseIdempotentTransaction(existingByKey.get(), order, method, payableAmount);
            }
        }

        Optional<PaymentTransaction> existingPending =
                transactionRepository.findFirstByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                        order.getId(), method, PaymentTransactionStatus.PENDING);

        if (existingPending.isPresent()) {
            PaymentTransaction oldTransaction = existingPending.get();

            boolean reusable = oldTransaction.getExpiresAt() != null
                    && oldTransaction.getExpiresAt().isAfter(AppTime.now())
                    && oldTransaction.getAmount().compareTo(payableAmount) == 0
                    && oldTransaction.getCheckoutUrl() != null;

            if (reusable) {
                log.info(
                        "[Payment Idempotency] Reusing pending {} transaction {} for order {}",
                        method,
                        oldTransaction.getId(),
                        order.getId());

                return paymentMapper.toCreateResponse(oldTransaction);
            }
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(payableAmount)
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(method)
                .businessDate(order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today())
                .idempotencyKey(idempotencyKey)
                .expiresAt(AppTime.now().plusMinutes(PAYMENT_LINK_EXPIRY_MINUTES))
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            PaymentGatewayCreateResult gatewayResult = gateway.createPaymentLink(transaction);

            transaction.setCheckoutUrl(gatewayResult.checkoutUrl());
            transaction.setQrCode(gatewayResult.qrCode());
            transaction.setExternalReference(gatewayResult.externalReference());
            transaction.setProviderPayload(gatewayResult.providerPayload());

            PaymentTransaction savedTransaction = transactionRepository.save(transaction);

            registerRollbackCancellation(savedTransaction, gateway);

            return paymentMapper.toCreateResponse(savedTransaction);
        } catch (Exception e) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);

            throw e;
        }
    }

    @Transactional
    public void cancelPaymentLink(@NonNull Long transactionId, String reason) {
        PaymentTransaction transaction = transactionRepository
                .findWithOrderByIdForUpdate(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
        lockTransactionOrder(transaction);

        if (transaction.getStatus() != PaymentTransactionStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_TRANSACTION_INVALID_STATE, "Only PENDING transactions can be cancelled");
        }

        String normalizedReason = normalizeCancelReason(reason);

        PaymentGateway gateway = gatewayResolver.resolve(transaction.getPaymentMethod());
        gateway.cancelPayment(transaction, normalizedReason);

        transaction.setStatus(PaymentTransactionStatus.CANCELLED);
        transaction.setFailureReason(normalizedReason);
        transactionRepository.save(transaction);

        log.info("Payment transaction {} cancelled", transactionId);
    }

    @Transactional
    public PaymentTransactionResponse syncPaymentStatus(@NonNull Long transactionId) {
        PaymentTransaction transaction = transactionRepository
                .findWithOrderByIdForUpdate(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
        lockTransactionOrder(transaction);

        if (transaction.getStatus() != PaymentTransactionStatus.PENDING) {
            return paymentMapper.toTransactionResponse(transaction);
        }

        PaymentGateway gateway = gatewayResolver.resolve(transaction.getPaymentMethod());
        PaymentGatewayStatusResult result = gateway.getPaymentStatus(transaction);

        applyGatewayStatus(transaction, result);

        return paymentMapper.toTransactionResponse(transaction);
    }

    @Transactional
    public void confirmPaymentFromWebhook(PaymentWebhookResult webhookResult) {
        if (webhookResult == null) {
            return;
        }

        PaymentTransaction existingTransaction = transactionRepository
                .findWithOrderById(webhookResult.transactionId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND,
                        "Transaction not found: " + webhookResult.transactionId()));

        Long orderId = existingTransaction.getOrder() != null
                ? existingTransaction.getOrder().getId()
                : null;

        if (orderId == null) {
            throw new BusinessException(
                    ErrorCode.ORDER_NOT_FOUND, "Payment transaction is not associated with an order");
        }

        Order lockedOrder = orderSettlementService.loadForPayment(orderId);

        PaymentTransaction transaction = transactionRepository
                .findWithOrderByIdForUpdate(webhookResult.transactionId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND,
                        "Transaction not found: " + webhookResult.transactionId()));
        transaction.setOrder(lockedOrder);

        if (transaction.getStatus() == PaymentTransactionStatus.PAID) {
            log.info("[Payment Webhook] Transaction {} already marked as PAID", transaction.getId());
            return;
        }

        if (transaction.getAmount().compareTo(webhookResult.amount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_WEBHOOK_INVALID, "Webhook amount does not match transaction");
        }

        transaction.setExternalReference(webhookResult.externalReference());
        transaction.setProviderPayload(webhookResult.providerPayload());

        if (transaction.getStatus() != PaymentTransactionStatus.PENDING) {
            Order order = transaction.getOrder();
            if (order != null && (order.isPaid() || order.isCompleted())) {
                log.warn(
                        "[Payment Webhook] Successful webhook received for inactive transaction {} on settled order {}",
                        transaction.getId(),
                        order.getId());
                paymentCompletionService.completeSuccessfulTransaction(transaction);
                return;
            }

            throw new BusinessException(
                    ErrorCode.PAYMENT_TRANSACTION_INVALID_STATE,
                    "Only PENDING transactions can be confirmed from webhook");
        }

        paymentCompletionService.completeSuccessfulTransaction(transaction);
    }

    private void applyGatewayStatus(PaymentTransaction transaction, PaymentGatewayStatusResult result) {

        if (result.status() == PaymentTransactionStatus.PAID) {
            transaction.setExternalReference(result.externalReference());
            transaction.setProviderPayload(result.providerPayload());

            paymentCompletionService.completeSuccessfulTransaction(transaction);
            return;
        }

        if (result.status() == PaymentTransactionStatus.CANCELLED
                || result.status() == PaymentTransactionStatus.EXPIRED
                || result.status() == PaymentTransactionStatus.FAILED) {

            transaction.setStatus(result.status());
            transaction.setExternalReference(result.externalReference());
            transaction.setProviderPayload(result.providerPayload());
            transaction.setFailureReason(result.failureReason());

            transactionRepository.save(transaction);
            return;
        }

        if (result.status() == PaymentTransactionStatus.PENDING
                && transaction.getExpiresAt() != null
                && transaction.getExpiresAt().isBefore(AppTime.now())) {

            transaction.setStatus(PaymentTransactionStatus.EXPIRED);
            transaction.setFailureReason("Payment link expired");
            transaction.setProviderPayload(result.providerPayload());

            transactionRepository.save(transaction);
        }
    }

    private PaymentCreateResponse reuseIdempotentTransaction(
            PaymentTransaction transaction, Order order, PaymentMethod method, BigDecimal amount) {

        boolean sameOrder = transaction.getOrder() != null
                && Objects.equals(transaction.getOrder().getId(), order.getId());

        boolean sameMethod = transaction.getPaymentMethod() == method;
        boolean sameAmount = transaction.getAmount().compareTo(amount) == 0;

        if (!sameOrder || !sameMethod || !sameAmount) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_IDEMPOTENCY_CONFLICT,
                    "Idempotency key is already associated with another payment request");
        }

        if (transaction.getStatus() == PaymentTransactionStatus.FAILED
                || transaction.getStatus() == PaymentTransactionStatus.CANCELLED
                || transaction.getStatus() == PaymentTransactionStatus.EXPIRED) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_IDEMPOTENCY_CONFLICT,
                    "Idempotency key is already associated with an inactive payment transaction");
        }

        return paymentMapper.toCreateResponse(transaction);
    }

    private void registerRollbackCancellation(PaymentTransaction transaction, PaymentGateway gateway) {

        sideEffects.afterRollback(
                () -> {
                    try {
                        gateway.cancelPayment(transaction, "Local transaction rolled back after payment link creation");
                    } catch (Exception e) {
                        log.error(
                                "Failed to cancel rolled back payment link {}: {}",
                                transaction.getId(),
                                e.getMessage());
                    }
                },
                "cancel rolled back payment link " + transaction.getId());
    }

    private void lockTransactionOrder(PaymentTransaction transaction) {
        if (transaction.getOrder() == null || transaction.getOrder().getId() == null) {
            throw new BusinessException(
                    ErrorCode.ORDER_NOT_FOUND, "Payment transaction is not associated with an order");
        }

        transaction.setOrder(
                orderSettlementService.loadForPayment(transaction.getOrder().getId()));
    }

    private BigDecimal currentFinalAmount(Order order) {
        return order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        return key.trim();
    }

    private String normalizeCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Customer changed payment method";
        }

        return reason.trim();
    }
}
