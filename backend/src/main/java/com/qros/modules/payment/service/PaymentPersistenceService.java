package com.qros.modules.payment.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.payment.dto.internal.OnlinePaymentPreparation;
import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentGatewayStatusResult;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPersistenceService {

    private static final int PAYMENT_LINK_EXPIRY_MINUTES = 20;
    private static final List<PaymentTransactionStatus> ONLINE_ACTIVE_STATUSES =
            List.of(PaymentTransactionStatus.CREATING, PaymentTransactionStatus.PENDING);

    private final PaymentTransactionRepository transactionRepository;
    private final OrderSettlementService orderSettlementService;
    private final PaymentCompletionService paymentCompletionService;
    private final UserRepository userRepository;

    @Transactional
    public OnlinePaymentPreparation prepareOnline(@NonNull PaymentCreateRequest request) {
        PaymentMethod method = request.paymentMethod();
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        if (idempotencyKey != null) {
            PaymentTransaction existing = transactionRepository
                    .findFirstByIdempotencyKey(idempotencyKey)
                    .orElse(null);
            if (existing != null) {
                return new OnlinePaymentPreparation(
                        validateReusable(
                                existing, Objects.requireNonNull(request.orderId()), method, existing.getAmount()),
                        false);
            }
        }

        Order order = orderSettlementService.prepareForPayment(
                Objects.requireNonNull(request.orderId()), request.voucherCode());
        BigDecimal payableAmount = currentFinalAmount(order);

        PaymentTransaction active = transactionRepository
                .findFirstByOrderIdAndPaymentMethodAndStatusInOrderByCreatedAtDesc(
                        order.getId(), method, ONLINE_ACTIVE_STATUSES)
                .orElse(null);
        if (active != null && isReusableActive(active, payableAmount)) {
            return new OnlinePaymentPreparation(active, false);
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(payableAmount)
                .status(PaymentTransactionStatus.CREATING)
                .paymentMethod(method)
                .businessDate(order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today())
                .idempotencyKey(idempotencyKey)
                .expiresAt(AppTime.now().plusMinutes(PAYMENT_LINK_EXPIRY_MINUTES))
                .build();

        return new OnlinePaymentPreparation(transactionRepository.saveAndFlush(transaction), true);
    }

    @Transactional
    public PaymentTransaction recoverIdempotency(@NonNull PaymentCreateRequest request) {
        String idempotencyKey = request.paymentMethod() == PaymentMethod.CASH
                ? cashIdempotencyKey(Objects.requireNonNull(request.orderId()), request.idempotencyKey())
                : normalizeIdempotencyKey(request.idempotencyKey());
        if (idempotencyKey == null) {
            throw new BusinessException(ErrorCode.PAYMENT_IDEMPOTENCY_CONFLICT);
        }
        PaymentTransaction existing = transactionRepository
                .findFirstByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_IDEMPOTENCY_CONFLICT));
        return validateReusable(
                existing, Objects.requireNonNull(request.orderId()), request.paymentMethod(), existing.getAmount());
    }

    @Transactional
    public PaymentTransaction completeOnlineCreation(
            @NonNull Long transactionId, @NonNull PaymentGatewayCreateResult result) {
        PaymentTransaction transaction = lockTransaction(transactionId);
        if (transaction.getStatus() == PaymentTransactionStatus.PAID) {
            return transaction;
        }
        if (transaction.getStatus() != PaymentTransactionStatus.CREATING) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_TRANSACTION_INVALID_STATE, "Payment transaction is no longer being created");
        }

        transaction.setCheckoutUrl(result.checkoutUrl());
        transaction.setQrCode(result.qrCode());
        transaction.setExternalReference(result.externalReference());
        transaction.setProviderPayload(result.providerPayload());
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public void markOnlineCreationFailed(@NonNull Long transactionId, String failureReason) {
        PaymentTransaction transaction = lockTransaction(transactionId);
        if (transaction.getStatus() != PaymentTransactionStatus.CREATING) {
            return;
        }
        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setFailureReason(normalizeFailureReason(failureReason));
        transactionRepository.save(transaction);
    }

    @Transactional
    public PaymentTransaction settleCash(@NonNull PaymentCreateRequest request, Long userId) {
        Long orderId = Objects.requireNonNull(request.orderId());
        String idempotencyKey = cashIdempotencyKey(orderId, request.idempotencyKey());

        PaymentTransaction existing =
                transactionRepository.findFirstByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return validateReusable(existing, orderId, PaymentMethod.CASH, existing.getAmount());
        }

        Order order = orderSettlementService.prepareForPayment(orderId, request.voucherCode());
        User currentUser = resolveUser(userId);
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(currentFinalAmount(order))
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH)
                .createdBy(currentUser)
                .businessDate(order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today())
                .idempotencyKey(idempotencyKey)
                .providerPayload("{\"provider\":\"CASH\",\"event\":\"MANUAL_PAYMENT\",\"reference\":\"\"}")
                .build();

        transaction = transactionRepository.saveAndFlush(transaction);
        paymentCompletionService.completeSuccessfulTransaction(transaction);
        cancelPendingOnlineTransactions(order.getId());
        return transaction;
    }

    @Transactional(readOnly = true)
    public PaymentTransaction loadForGatewayOperation(@NonNull Long transactionId) {
        return transactionRepository
                .findWithOrderById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
    }

    @Transactional
    public PaymentTransaction markCancelled(@NonNull Long transactionId, @NonNull String reason) {
        PaymentTransaction transaction = lockTransaction(transactionId);
        if (transaction.getStatus() == PaymentTransactionStatus.PAID) {
            return transaction;
        }
        if (!ONLINE_ACTIVE_STATUSES.contains(transaction.getStatus())) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_TRANSACTION_INVALID_STATE, "Only active online transactions can be cancelled");
        }
        transaction.setStatus(PaymentTransactionStatus.CANCELLED);
        transaction.setFailureReason(reason);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public PaymentTransaction applyGatewayStatus(
            @NonNull Long transactionId, @NonNull PaymentGatewayStatusResult result) {
        PaymentTransaction transaction = lockTransaction(transactionId);
        lockTransactionOrder(transaction);

        if (result.status() == PaymentTransactionStatus.PAID) {
            transaction.setExternalReference(result.externalReference());
            transaction.setProviderPayload(result.providerPayload());
            paymentCompletionService.completeSuccessfulTransaction(transaction);
            return transaction;
        }

        if (result.status() == PaymentTransactionStatus.CANCELLED
                || result.status() == PaymentTransactionStatus.EXPIRED
                || result.status() == PaymentTransactionStatus.FAILED) {
            transaction.setStatus(result.status());
            transaction.setExternalReference(result.externalReference());
            transaction.setProviderPayload(result.providerPayload());
            transaction.setFailureReason(result.failureReason());
            return transactionRepository.save(transaction);
        }

        if (result.status() == PaymentTransactionStatus.PENDING) {
            if (transaction.getExpiresAt() != null && transaction.getExpiresAt().isBefore(AppTime.now())) {
                transaction.setStatus(PaymentTransactionStatus.EXPIRED);
                transaction.setFailureReason("Payment link expired");
            }
            transaction.setProviderPayload(result.providerPayload());
            return transactionRepository.save(transaction);
        }

        return transaction;
    }

    @Transactional
    public void confirmWebhook(PaymentWebhookResult webhookResult) {
        if (webhookResult == null) {
            return;
        }

        PaymentTransaction transaction = lockTransaction(webhookResult.transactionId());
        lockTransactionOrder(transaction);

        if (transaction.getStatus() == PaymentTransactionStatus.PAID) {
            return;
        }
        if (transaction.getAmount().compareTo(webhookResult.amount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_WEBHOOK_INVALID, "Webhook amount does not match transaction");
        }

        transaction.setExternalReference(webhookResult.externalReference());
        transaction.setProviderPayload(webhookResult.providerPayload());

        if (!ONLINE_ACTIVE_STATUSES.contains(transaction.getStatus())) {
            Order order = transaction.getOrder();
            if (order != null && (order.isPaid() || order.isCompleted())) {
                paymentCompletionService.completeSuccessfulTransaction(transaction);
                return;
            }
            throw new BusinessException(
                    ErrorCode.PAYMENT_TRANSACTION_INVALID_STATE,
                    "Only active online transactions can be confirmed from webhook");
        }

        paymentCompletionService.completeSuccessfulTransaction(transaction);
    }

    private PaymentTransaction validateReusable(
            PaymentTransaction transaction, Long orderId, PaymentMethod method, BigDecimal amount) {
        boolean sameOrder = transaction.getOrder() != null
                && Objects.equals(transaction.getOrder().getId(), orderId);
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
        return transaction;
    }

    private boolean isReusableActive(PaymentTransaction transaction, BigDecimal payableAmount) {
        if (transaction.getExpiresAt() == null
                || !transaction.getExpiresAt().isAfter(AppTime.now())
                || transaction.getAmount().compareTo(payableAmount) != 0) {
            return false;
        }
        return transaction.getStatus() == PaymentTransactionStatus.CREATING || transaction.getCheckoutUrl() != null;
    }

    private PaymentTransaction lockTransaction(Long transactionId) {
        return transactionRepository
                .findWithOrderByIdForUpdate(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
    }

    private void lockTransactionOrder(PaymentTransaction transaction) {
        if (transaction.getOrder() == null || transaction.getOrder().getId() == null) {
            throw new BusinessException(
                    ErrorCode.ORDER_NOT_FOUND, "Payment transaction is not associated with an order");
        }
        transaction.setOrder(
                orderSettlementService.loadForPayment(transaction.getOrder().getId()));
    }

    private void cancelPendingOnlineTransactions(Long orderId) {
        for (PaymentTransaction transaction : transactionRepository.findPendingOnlineTransactionsByOrderId(orderId)) {
            transaction.setStatus(PaymentTransactionStatus.CANCELLED);
            transaction.setFailureReason("Cancelled because order was paid by cash");
            transaction.setUpdatedAt(AppTime.now());
        }
    }

    private User resolveUser(Long userId) {
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }

    private BigDecimal currentFinalAmount(Order order) {
        return order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
    }

    private String cashIdempotencyKey(Long orderId, String requestedKey) {
        String normalizedKey = normalizeIdempotencyKey(requestedKey);
        return normalizedKey != null ? normalizedKey : "cash:order:" + orderId;
    }

    private String normalizeIdempotencyKey(String key) {
        return key == null || key.isBlank() ? null : key.trim();
    }

    private String normalizeFailureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Payment gateway request failed";
        }
        return reason.length() <= 255 ? reason : reason.substring(0, 255);
    }
}
