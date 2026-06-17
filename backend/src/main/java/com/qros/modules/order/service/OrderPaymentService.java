package com.qros.modules.order.service;

import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.shared.enums.PaymentMethod;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.payment.service.PaymentCompletionService;
import com.qros.modules.promotion.dto.internal.VoucherPaymentResult;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final VoucherCheckoutService voucherCheckoutService;
    private final PaymentTransactionRepository transactionRepository;
    private final OrderPricingService orderPricingService;
    private final PaymentCompletionService paymentCompletionService;
    private final OrderStatusService orderStatusService;
    private final OrderMapper orderMapper;

    @Transactional
    public String payOrder(@NonNull Long id, String voucherCode) {
        return payOrder(id, null, voucherCode);
    }

    @Transactional
    public String payOrder(@NonNull Long id, Long userId, String voucherCode) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "This order is already settled");
        }

        orderStatusService.tryAutoPromoteOrder(order);

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID,
                    "Order must be in AWAITING_PAYMENT status before payment. Current: " + order.getStatus());
        }

        User currentUser = resolveUser(userId);

        VoucherPaymentResult voucherResult = applyVoucher(order, voucherCode);
        BigDecimal finalAmount = currentFinalAmount(order);

        PaymentTransaction transaction = createCashTransactionIfMissing(
                order,
                finalAmount,
                currentUser,
                "cash:order:" + order.getId());

        paymentCompletionService.completeSuccessfulTransaction(transaction, voucherResult);
        cancelPendingOnlineTransactions(order.getId());

        log.info("Order #{} settled via CASH", id);

        return "Order settled successfully";
    }

    @Transactional
    public OrderResponse confirmPaid(@NonNull Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return orderMapper.toResponse(order);
        }

        orderStatusService.tryAutoPromoteOrder(order);

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID,
                    "Order must be in AWAITING_PAYMENT status before confirming payment. Current: "
                            + order.getStatus());
        }

        BigDecimal finalAmount = currentFinalAmount(order);

        PaymentTransaction transaction = createCashTransactionIfMissing(
                order,
                finalAmount,
                null,
                "manual-confirm:order:" + order.getId());

        Order saved = paymentCompletionService.completeSuccessfulTransaction(transaction);

        return orderMapper.toResponse(saved);
    }

    private BigDecimal currentFinalAmount(Order order) {
        return order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
    }

    private BigDecimal currentSubtotalAmount(Order order) {
        return order.getSubtotalAmount() != null ? order.getSubtotalAmount() : BigDecimal.ZERO;
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }

        return userRepository.findById(userId).orElse(null);
    }

    private VoucherPaymentResult applyVoucher(Order order, String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }

        VoucherPaymentResult voucherResult = voucherCheckoutService.resolveForPayment(voucherCode, currentSubtotalAmount(order));
        order.setVoucherCode(voucherResult.voucherCode());
        orderPricingService.setOrderMoney(order, currentSubtotalAmount(order), voucherResult.appliedDiscountAmount());

        return voucherResult;
    }

    private PaymentTransaction createCashTransactionIfMissing(
            Order order,
            BigDecimal amount,
            User createdBy,
            String idempotencyKey) {

        return transactionRepository.findFirstByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createCashTransaction(order, amount, createdBy, idempotencyKey));
    }

    private PaymentTransaction createCashTransaction(
            Order order,
            BigDecimal amount,
            User createdBy,
            String idempotencyKey) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(amount)
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH)
                .createdBy(createdBy)
                .businessDate(order.getBusinessDate() != null ? order.getBusinessDate() : AppTime.today())
                .idempotencyKey(idempotencyKey)
                .providerPayload("{\"provider\":\"CASH\",\"event\":\"MANUAL_PAYMENT\",\"reference\":\"\"}")
                .build();

        return transactionRepository.save(transaction);
    }

    private void cancelPendingOnlineTransactions(Long orderId) {
        for (PaymentTransaction transaction : transactionRepository.findPendingOnlineTransactionsByOrderId(orderId)) {
            transaction.setStatus(PaymentTransactionStatus.CANCELLED);
            transaction.setFailureReason("Cancelled because order was paid by cash");
            transaction.setUpdatedAt(AppTime.now());
        }
    }
}
