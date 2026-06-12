package com.qros.modules.payment.service;

import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderAuditService;
import com.qros.modules.order.service.OrderTableSyncService;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.dto.internal.VoucherPaymentResult;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.service.OrderDiscountService;
import com.qros.modules.promotion.service.VoucherService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompletionService {

    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final OrderStateFactory orderStateFactory;

    private final OrderAuditService orderAuditService;
    private final OrderDiscountService orderDiscountService;
    private final VoucherService voucherService;

    private final OrderTableSyncService orderTableSyncService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final NotificationService notificationService;

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
    public Order completeSuccessfulTransaction(
            @NonNull PaymentTransaction transaction,
            VoucherPaymentResult voucherPaymentResult) {
        Order order = transaction.getOrder();

        if (order == null) {
            throw new BusinessException(
                    ErrorCode.ORDER_NOT_FOUND,
                    "Payment transaction is not associated with an order");
        }

        order = orderRepository.findByIdForUpdate(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
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

        Order savedOrder = settleOrderIfNeeded(order, transaction, totalPaid, voucherPaymentResult);

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

    private Order settleOrderIfNeeded(
            Order order,
            PaymentTransaction transaction,
            BigDecimal totalPaid,
            VoucherPaymentResult voucherPaymentResult) {

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return order;
        }

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID,
                    "Order must be in AWAITING_PAYMENT status before payment completion. Current: "
                            + order.getStatus());
        }

        OrderStatus fromStatus = order.getStatus();

        transitionOrderToCompleted(order, fromStatus);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod(transaction.getPaymentMethod());
        order.setPaymentTime(AppTime.now());
        order.setPaidBy(transaction.getCreatedBy());
        order.setPaidAmount(totalPaid);
        order.setBusinessDate(order.getPaymentTime().toLocalDate());

        Order savedOrder = orderRepository.save(order);
        recordAndConsumeVoucher(savedOrder, voucherPaymentResult);

        orderAuditService.recordOrderStatus(
                savedOrder,
                fromStatus,
                savedOrder.getStatus(),
                transaction.getCreatedBy(),
                "payment-" + transaction.getPaymentMethod().name().toLowerCase());

        orderTableSyncService.recalcTableStatus(savedOrder);
        orderCacheInvalidationService.evictAfterOrderMutation(savedOrder.getId());

        notificationService.notifyOrderChange();

        log.info(
                "Order #{} successfully completed via {} payment",
                savedOrder.getId(),
                transaction.getPaymentMethod());

        return savedOrder;
    }

    private void transitionOrderToCompleted(Order order, OrderStatus fromStatus) {
        try {
            orderStateFactory.validateTransition(fromStatus, OrderStatus.COMPLETED);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE, e.getMessage());
        }

        OrderState state = orderStateFactory.getState(OrderStatus.COMPLETED);
        state.handleRequest(order);
    }

    private void notifyPaymentSuccess(PaymentTransaction transaction) {
        Order order = transaction.getOrder();

        if (order == null) {
            return;
        }

        notificationService.notifyPaymentSuccess(order.getId(), transaction.getId());
    }

    private BigDecimal currentFinalAmount(Order order) {
        return order.getFinalAmount() != null
                ? order.getFinalAmount()
                : BigDecimal.ZERO;
    }

    private BigDecimal currentSubtotalAmount(Order order) {
        return order.getSubtotalAmount() != null
                ? order.getSubtotalAmount()
                : BigDecimal.ZERO;
    }

    private void recordAndConsumeVoucher(Order order, VoucherPaymentResult voucherPaymentResult) {
        if (order.getVoucherCode() == null || order.getVoucherCode().isBlank()) {
            return;
        }

        VoucherPaymentResult resolvedVoucherResult = voucherPaymentResult != null
                ? voucherPaymentResult
                : voucherService.snapshotForSettledOrder(
                        order.getVoucherCode(),
                        currentSubtotalAmount(order),
                        order.getDiscountAmount(),
                        currentFinalAmount(order));

        if (resolvedVoucherResult == null || resolvedVoucherResult.appliedDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Voucher voucher = resolvedVoucherResult.voucher();
        DiscountResult result = resolvedVoucherResult.discountResult();

        orderDiscountService.recordVoucherDiscount(order, voucher, result);
        voucherService.incrementUsage(voucher.getId());
    }

}
