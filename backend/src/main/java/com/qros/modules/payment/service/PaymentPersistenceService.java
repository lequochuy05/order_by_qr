package com.qros.modules.payment.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.payment.dto.internal.CashSettlementResult;
import com.qros.modules.payment.dto.internal.OnlinePaymentPreparation;
import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentGatewayStatusResult;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.promotion.service.VoucherService;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PaymentPersistenceService {

    private static final List<PaymentTransactionStatus> ONLINE_ACTIVE_STATUSES =
            List.of(PaymentTransactionStatus.CREATING, PaymentTransactionStatus.PENDING);
    private static final long PAYOS_ORDER_CODE_SUFFIX_SIZE = 1_000_000L;

    private final PaymentTransactionRepository transactionRepository;
    private final OrderSettlementService orderSettlementService;
    private final PaymentCompletionService paymentCompletionService;
    private final UserLookupService userRepository;
    private final SystemSettingsService settingsService;
    private final VoucherService voucherService;

    public PaymentPersistenceService(
            PaymentTransactionRepository transactionRepository,
            OrderSettlementService orderSettlementService,
            PaymentCompletionService paymentCompletionService,
            UserLookupService userRepository,
            SystemSettingsService settingsService) {
        this(
                transactionRepository,
                orderSettlementService,
                paymentCompletionService,
                userRepository,
                settingsService,
                null);
    }

    @Autowired
    public PaymentPersistenceService(
            PaymentTransactionRepository transactionRepository,
            OrderSettlementService orderSettlementService,
            PaymentCompletionService paymentCompletionService,
            UserLookupService userRepository,
            SystemSettingsService settingsService,
            VoucherService voucherService) {
        this.transactionRepository = transactionRepository;
        this.orderSettlementService = orderSettlementService;
        this.paymentCompletionService = paymentCompletionService;
        this.userRepository = userRepository;
        this.settingsService = settingsService;
        this.voucherService = voucherService;
    }

    @Transactional
    public OnlinePaymentPreparation prepareOnline(@NonNull PaymentCreateRequest request) {
        PaymentMethod method = request.paymentMethod();
        Long orderId = Objects.requireNonNull(request.orderId());
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        if (idempotencyKey != null) {
            PaymentTransaction existing = transactionRepository
                    .findFirstByIdempotencyKey(idempotencyKey)
                    .orElse(null);
            if (existing != null) {
                return new OnlinePaymentPreparation(validateReusable(existing, orderId, method), false);
            }
        }

        // Expire any already-lapsed active online transactions for this order before validating voucher capacity,
        // so retrying with a single-use voucher does not falsely fail while waiting for the 5-minute cleanup cron.
        List<PaymentTransaction> expiredOnOrder = expireLapsedOnlineTransactions(orderId);

        Order order = Boolean.TRUE.equals(request.confirmUnfinishedItems())
                ? orderSettlementService.prepareForPayment(orderId, request.voucherCode(), true)
                : orderSettlementService.prepareForPayment(orderId, request.voucherCode());
        BigDecimal payableAmount = currentFinalAmount(order);

        if (payableAmount.compareTo(BigDecimal.ZERO) == 0) {
            List<PaymentTransaction> cancelledOnline = cancelPendingOnlineTransactions(
                    order.getId(), "Cancelled because order settled with zero payable amount");
            PaymentTransaction zeroTransaction = PaymentTransaction.builder()
                    .order(order)
                    .amount(BigDecimal.ZERO)
                    .status(PaymentTransactionStatus.PENDING)
                    .paymentMethod(method)
                    .businessDate(order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today())
                    .idempotencyKey(idempotencyKey)
                    .voucherCodeSnapshot(order.getVoucherCode())
                    .providerPayload("{\"provider\":\"INTERNAL\",\"event\":\"ZERO_AMOUNT_SETTLEMENT\"}")
                    .build();
            zeroTransaction = transactionRepository.saveAndFlush(zeroTransaction);
            paymentCompletionService.completeSuccessfulTransaction(zeroTransaction);
            return new OnlinePaymentPreparation(zeroTransaction, false, combineLists(expiredOnOrder, cancelledOnline));
        }

        PaymentTransaction active = transactionRepository
                .findFirstByOrderIdAndPaymentMethodAndStatusInOrderByCreatedAtDesc(
                        order.getId(), method, ONLINE_ACTIVE_STATUSES)
                .orElse(null);
        if (active != null && isReusableActive(active, payableAmount)) {
            return new OnlinePaymentPreparation(active, false, expiredOnOrder);
        }

        List<PaymentTransaction> supersededOnline =
                cancelPendingOnlineTransactions(order.getId(), "Superseded by a new online payment request");

        boolean voucherReserved = false;
        String voucherCodeSnapshot = order.getVoucherCode();
        if (voucherService != null && voucherCodeSnapshot != null && !voucherCodeSnapshot.isBlank()) {
            if (!voucherService.tryReserveUsageByCode(voucherCodeSnapshot)) {
                throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
            }
            voucherReserved = true;
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(payableAmount)
                .status(PaymentTransactionStatus.CREATING)
                .paymentMethod(method)
                .businessDate(order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today())
                .idempotencyKey(idempotencyKey)
                .voucherUsageReserved(voucherReserved)
                .voucherCodeSnapshot(voucherCodeSnapshot)
                .expiresAt(AppTime.now()
                        .plusMinutes(settingsService.getSettingsEntity().getPaymentQrExpiresInMinutes()))
                .build();

        transaction = transactionRepository.saveAndFlush(transaction);
        transaction.setGatewayOrderCode(generateGatewayOrderCode(transaction.getId()));
        return new OnlinePaymentPreparation(
                transactionRepository.saveAndFlush(transaction), true, combineLists(expiredOnOrder, supersededOnline));
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
        Long orderId = Objects.requireNonNull(request.orderId());
        return validateReusable(existing, orderId, request.paymentMethod());
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
        releaseVoucherReservationIfHeld(transaction);
        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setFailureReason(normalizeFailureReason(failureReason));
        transactionRepository.save(transaction);
    }

    @Transactional
    public PaymentTransaction expireOrphanedCreatingTransaction(@NonNull Long transactionId, String reason) {
        PaymentTransaction transaction = lockTransaction(transactionId);
        if (!ONLINE_ACTIVE_STATUSES.contains(transaction.getStatus())) {
            return transaction;
        }
        releaseVoucherReservationIfHeld(transaction);
        transaction.setStatus(PaymentTransactionStatus.EXPIRED);
        transaction.setFailureReason(normalizeFailureReason(reason));
        return transactionRepository.save(transaction);
    }

    @Transactional
    public CashSettlementResult settleCash(@NonNull PaymentCreateRequest request, Long userId) {
        Long orderId = Objects.requireNonNull(request.orderId());
        String idempotencyKey = cashIdempotencyKey(orderId, request.idempotencyKey());

        PaymentTransaction existing =
                transactionRepository.findFirstByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return new CashSettlementResult(validateReusable(existing, orderId, PaymentMethod.CASH), List.of());
        }

        boolean confirmUnfinished = Boolean.TRUE.equals(request.confirmUnfinishedItems());
        Order order = orderSettlementService.prepareForPayment(orderId, request.voucherCode(), confirmUnfinished);
        BigDecimal payableAmount = currentFinalAmount(order);
        validateCashReceived(request.cashReceived(), payableAmount);
        User currentUser = resolveUser(userId);

        String providerPayload = buildCashProviderPayload(request, payableAmount);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(payableAmount)
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH)
                .createdBy(currentUser)
                .businessDate(order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today())
                .idempotencyKey(idempotencyKey)
                .providerPayload(providerPayload)
                .build();

        transaction = transactionRepository.saveAndFlush(transaction);
        paymentCompletionService.completeSuccessfulCashTransaction(transaction, order);
        List<PaymentTransaction> cancelledOnlineTransactions =
                cancelPendingOnlineTransactions(order.getId(), "Cancelled because order was paid by cash");
        return new CashSettlementResult(transaction, cancelledOnlineTransactions);
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
        releaseVoucherReservationIfHeld(transaction);
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
            releaseVoucherReservationIfHeld(transaction);
            transaction.setStatus(result.status());
            transaction.setExternalReference(result.externalReference());
            transaction.setProviderPayload(result.providerPayload());
            transaction.setFailureReason(result.failureReason());
            return transactionRepository.save(transaction);
        }

        if (result.status() == PaymentTransactionStatus.PENDING) {
            if (transaction.getExpiresAt() != null && transaction.getExpiresAt().isBefore(AppTime.now())) {
                releaseVoucherReservationIfHeld(transaction);
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

        PaymentTransaction transaction = lockTransactionByGatewayOrderCode(webhookResult.gatewayOrderCode());
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
            if (order != null && (order.isPaid() || order.isCompleted() || order.isCancelled())) {
                paymentCompletionService.completeSuccessfulTransaction(transaction);
                return;
            }
            // Late webhook on a locally cancelled/expired transaction where the order is still open:
            // record payment for manual reconciliation instead of rolling back money received.
            transaction.setStatus(PaymentTransactionStatus.PAID);
            if (transaction.getPaidAt() == null) {
                transaction.setPaidAt(AppTime.now());
            }
            if (transaction.getBusinessDate() == null) {
                transaction.setBusinessDate(transaction.getPaidAt().toLocalDate());
            }
            releaseVoucherReservationIfHeld(transaction);
            transaction.setFailureReason(
                    "Late payment received for inactive transaction; manual refund or reconciliation required");
            transactionRepository.save(transaction);
            log.error(
                    "Late webhook confirmed on inactive transaction {} for order {}; manual reconciliation required",
                    transaction.getId(),
                    order != null ? order.getId() : null);
            return;
        }

        paymentCompletionService.completeSuccessfulTransaction(transaction);
    }

    private PaymentTransaction validateReusable(PaymentTransaction transaction, Long orderId, PaymentMethod method) {
        boolean sameOrder = transaction.getOrder() != null
                && Objects.equals(transaction.getOrder().getId(), orderId);
        boolean sameMethod = transaction.getPaymentMethod() == method;

        if (!sameOrder || !sameMethod) {
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

        BigDecimal currentAmount = currentPayableAmountForReuse(transaction, orderId);
        if (transaction.getAmount().compareTo(currentAmount) != 0) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_IDEMPOTENCY_CONFLICT,
                    "Idempotency key is already associated with another payment request");
        }
        return transaction;
    }

    private BigDecimal currentPayableAmountForReuse(PaymentTransaction transaction, Long orderId) {
        if (transaction.getStatus() == PaymentTransactionStatus.PAID) {
            return transaction.getAmount();
        }
        Order order = orderSettlementService.loadForPayment(orderId);
        return currentFinalAmount(order);
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

    private PaymentTransaction lockTransactionByGatewayOrderCode(Long gatewayOrderCode) {
        return transactionRepository
                .findWithOrderByGatewayOrderCodeForUpdate(gatewayOrderCode)
                .orElseGet(() -> lockTransaction(gatewayOrderCode));
    }

    private void lockTransactionOrder(PaymentTransaction transaction) {
        if (transaction.getOrder() == null || transaction.getOrder().getId() == null) {
            throw new BusinessException(
                    ErrorCode.ORDER_NOT_FOUND, "Payment transaction is not associated with an order");
        }
        transaction.setOrder(
                orderSettlementService.loadForPayment(transaction.getOrder().getId()));
    }

    private List<PaymentTransaction> expireLapsedOnlineTransactions(Long orderId) {
        List<PaymentTransaction> transactions = transactionRepository.findPendingOnlineTransactionsByOrderId(orderId);
        if (transactions.isEmpty()) {
            return List.of();
        }
        java.time.LocalDateTime now = AppTime.now();
        List<PaymentTransaction> expired = new java.util.ArrayList<>();
        for (PaymentTransaction transaction : transactions) {
            if (transaction.getExpiresAt() != null
                    && !transaction.getExpiresAt().isAfter(now)) {
                releaseVoucherReservationIfHeld(transaction);
                transaction.setStatus(PaymentTransactionStatus.EXPIRED);
                transaction.setFailureReason("Payment link expired before retry");
                transaction.setUpdatedAt(now);
                expired.add(transaction);
            }
        }
        if (!expired.isEmpty()) {
            transactionRepository.saveAll(expired);
        }
        return expired;
    }

    private List<PaymentTransaction> cancelPendingOnlineTransactions(Long orderId, String reason) {
        List<PaymentTransaction> transactions = transactionRepository.findPendingOnlineTransactionsByOrderId(orderId);
        for (PaymentTransaction transaction : transactions) {
            releaseVoucherReservationIfHeld(transaction);
            transaction.setStatus(PaymentTransactionStatus.CANCELLED);
            transaction.setFailureReason(reason);
            transaction.setUpdatedAt(AppTime.now());
        }
        if (!transactions.isEmpty()) {
            transactionRepository.saveAll(transactions);
        }
        return transactions;
    }

    private List<PaymentTransaction> combineLists(List<PaymentTransaction> first, List<PaymentTransaction> second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        List<PaymentTransaction> combined = new java.util.ArrayList<>(first.size() + second.size());
        combined.addAll(first);
        combined.addAll(second);
        return combined;
    }

    private void validateCashReceived(BigDecimal cashReceived, BigDecimal payableAmount) {
        if (cashReceived == null) {
            return;
        }
        if (cashReceived.compareTo(payableAmount) < 0) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID, "Cash received cannot be less than payable amount");
        }
    }

    private void releaseVoucherReservationIfHeld(PaymentTransaction transaction) {
        if (voucherService != null
                && Boolean.TRUE.equals(transaction.getVoucherUsageReserved())
                && !Boolean.TRUE.equals(transaction.getVoucherUsageClaimed())) {
            String code = transaction.getVoucherCodeSnapshot() != null
                    ? transaction.getVoucherCodeSnapshot()
                    : (transaction.getOrder() != null ? transaction.getOrder().getVoucherCode() : null);
            voucherService.releaseReservedUsageByCode(code);
            transaction.setVoucherUsageReserved(false);
        }
    }

    private User resolveUser(Long userId) {
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }

    private BigDecimal currentFinalAmount(Order order) {
        return order.getEffectiveFinalAmount();
    }

    private String buildCashProviderPayload(PaymentCreateRequest request, BigDecimal payableAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"provider\":\"CASH\",\"event\":\"MANUAL_PAYMENT\",\"reference\":\"\"");
        if (request.cashReceived() != null) {
            BigDecimal computedChange =
                    request.cashReceived().subtract(payableAmount).max(BigDecimal.ZERO);
            sb.append(",\"cashReceived\":").append(request.cashReceived().toPlainString());
            sb.append(",\"changeAmount\":").append(computedChange.toPlainString());
        }
        sb.append('}');
        return sb.toString();
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

    private Long generateGatewayOrderCode(Long transactionId) {
        long suffix = Math.floorMod(Objects.requireNonNull(transactionId), PAYOS_ORDER_CODE_SUFFIX_SIZE);
        return (System.currentTimeMillis() / 1000L) * PAYOS_ORDER_CODE_SUFFIX_SIZE + suffix;
    }
}
