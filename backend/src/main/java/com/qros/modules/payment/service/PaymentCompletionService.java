package com.qros.modules.payment.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.dto.internal.VoucherPaymentResult;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.service.OrderDiscountService;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.promotion.service.VoucherService;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompletionService {

    private final PaymentTransactionRepository transactionRepository;
    private final OrderSettlementService orderSettlementService;

    private final OrderDiscountService orderDiscountService;
    private final VoucherService voucherService;
    private final VoucherCheckoutService voucherCheckoutService;
    private final ApplicationEventPublisher eventPublisher;

    private final MeterRegistry meterRegistry;

    private Counter paymentsCompletedCounter;

    @PostConstruct
    public void initCounters() {
        paymentsCompletedCounter = Counter.builder("payments.completed")
                .description("Total number of successful payment transactions")
                .register(meterRegistry);
    }

    @Transactional
    public Order completeSuccessfulTransaction(@NonNull PaymentTransaction transaction) {
        return completeSuccessfulTransaction(transaction, null);
    }

    @Transactional
    public Order completeSuccessfulCashTransaction(
            @NonNull PaymentTransaction transaction, @NonNull Order lockedOrder) {
        return completeSuccessfulTransaction(transaction, null, lockedOrder);
    }

    @Transactional
    public Order completeSuccessfulTransaction(
            @NonNull PaymentTransaction transaction, VoucherPaymentResult voucherPaymentResult) {
        return completeSuccessfulTransaction(transaction, voucherPaymentResult, null);
    }

    private Order completeSuccessfulTransaction(
            @NonNull PaymentTransaction transaction, VoucherPaymentResult voucherPaymentResult, Order lockedOrder) {
        Order order = transaction.getOrder();

        if (order == null) {
            throw new BusinessException(
                    ErrorCode.ORDER_NOT_FOUND, "Payment transaction is not associated with an order");
        }

        order = lockedOrder != null ? lockedOrder : orderSettlementService.loadForPayment(order.getId());
        transaction.setOrder(order);

        if (order.isPaid() || order.isCompleted()) {
            markDuplicatePaymentIfNeeded(transaction);
            return order;
        }

        if (transaction.getStatus() != PaymentTransactionStatus.PAID) {
            markTransactionPaid(transaction);
        }

        BigDecimal totalPaid = transactionRepository.sumPaidAmountByOrderId(order.getId());
        BigDecimal totalBill = currentFinalAmount(order);

        log.info(
                "[Payment Completion] Order #{} | Total paid: {} | Total bill: {}",
                order.getId(),
                totalPaid,
                totalBill);

        if (totalPaid.compareTo(totalBill) < 0) {
            notifyPaymentSuccess(transaction);
            paymentsCompletedCounter.increment();
            return order;
        }

        VoucherClaimResult voucherClaim = claimVoucherUsage(order, transaction, voucherPaymentResult);
        if (!voucherClaim.settlementAllowed()) {
            return order;
        }

        Order savedOrder = settleOrderIfNeeded(order, transaction, totalPaid);
        recordVoucherDiscount(savedOrder, voucherClaim.voucherPaymentResult());

        notifyPaymentSuccess(transaction);
        paymentsCompletedCounter.increment();

        return savedOrder;
    }

    private void markTransactionPaid(PaymentTransaction transaction) {
        transaction.setStatus(PaymentTransactionStatus.PAID);

        if (transaction.getPaidAt() == null) {
            transaction.setPaidAt(AppTime.now());
        }

        if (transaction.getBusinessDate() == null) {
            transaction.setBusinessDate(transaction.getPaidAt().toLocalDate());
        }

        transactionRepository.save(transaction);
    }

    private void markDuplicatePaymentIfNeeded(PaymentTransaction transaction) {
        if (transaction.getStatus() == PaymentTransactionStatus.PAID) {
            return;
        }

        transaction.setStatus(PaymentTransactionStatus.PAID);

        if (transaction.getPaidAt() == null) {
            transaction.setPaidAt(AppTime.now());
        }

        if (transaction.getBusinessDate() == null) {
            transaction.setBusinessDate(transaction.getPaidAt().toLocalDate());
        }

        transaction.setFailureReason("Duplicate payment: order already completed, requires manual refund");
        transactionRepository.save(transaction);

        log.warn(
                "Payment transaction {} marked as duplicate for already settled order {}",
                transaction.getId(),
                transaction.getOrder() != null ? transaction.getOrder().getId() : null);
    }

    private Order settleOrderIfNeeded(Order order, PaymentTransaction transaction, BigDecimal totalPaid) {
        String auditReason = "payment-" + transaction.getPaymentMethod().name().toLowerCase();
        return orderSettlementService.settleAfterPayment(
                order, transaction.getPaymentMethod(), transaction.getCreatedBy(), totalPaid, auditReason);
    }

    private void notifyPaymentSuccess(PaymentTransaction transaction) {
        Order order = transaction.getOrder();

        if (order == null) {
            return;
        }

        eventPublisher.publishEvent(new PaymentSuccessEvent(order.getId(), transaction.getId()));
    }

    private BigDecimal currentFinalAmount(Order order) {
        return order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
    }

    private VoucherClaimResult claimVoucherUsage(
            Order order, PaymentTransaction transaction, VoucherPaymentResult voucherPaymentResult) {
        if (transaction.getVoucherUsageClaimed()) {
            return VoucherClaimResult.allowed(voucherPaymentResult);
        }
        if (order.getVoucherCode() == null || order.getVoucherCode().isBlank()) {
            return VoucherClaimResult.allowed(voucherPaymentResult);
        }

        VoucherPaymentResult resolvedVoucherResult = voucherPaymentResult != null
                ? voucherPaymentResult
                : voucherCheckoutService.snapshotForSettledOrder(
                        order.getVoucherCode(),
                        order.getSubtotalAmount(),
                        order.getDiscountAmount(),
                        currentFinalAmount(order));
        if (resolvedVoucherResult == null
                || resolvedVoucherResult.appliedDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return VoucherClaimResult.allowed(resolvedVoucherResult);
        }

        Voucher voucher = resolvedVoucherResult.voucher();
        if (!voucherService.tryIncrementUsage(voucher.getId())) {
            transaction.setFailureReason(
                    "Voucher capacity was exhausted after payment confirmation; manual refund required");
            transactionRepository.save(transaction);
            log.error(
                    "Payment transaction {} is paid but order {} cannot settle because voucher {} is exhausted",
                    transaction.getId(),
                    order.getId(),
                    voucher.getCode());
            return VoucherClaimResult.reconciliationRequired();
        }

        transaction.setVoucherUsageClaimed(true);
        transactionRepository.save(transaction);
        return VoucherClaimResult.allowed(resolvedVoucherResult);
    }

    private void recordVoucherDiscount(Order order, VoucherPaymentResult voucherPaymentResult) {
        if (voucherPaymentResult == null
                || voucherPaymentResult.appliedDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Voucher voucher = voucherPaymentResult.voucher();
        DiscountResult result = voucherPaymentResult.discountResult();
        orderDiscountService.recordVoucherDiscount(order, voucher, result);
    }

    private record VoucherClaimResult(boolean settlementAllowed, VoucherPaymentResult voucherPaymentResult) {
        private static VoucherClaimResult allowed(VoucherPaymentResult voucherPaymentResult) {
            return new VoucherClaimResult(true, voucherPaymentResult);
        }

        private static VoucherClaimResult reconciliationRequired() {
            return new VoucherClaimResult(false, null);
        }
    }
}
