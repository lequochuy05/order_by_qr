package com.qros.modules.order.service;

import org.springframework.context.ApplicationEventPublisher;
import com.qros.shared.event.DomainEvents.*;
import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.promotion.dto.internal.VoucherPaymentResult;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.service.TableSessionService;
import com.qros.modules.user.model.User;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSettlementService {

    private final OrderRepository orderRepository;
    private final OrderStateFactory orderStateFactory;
    private final OrderAuditService orderAuditService;
    private final OrderTableSyncService orderTableSyncService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderPricingService orderPricingService;
    private final VoucherCheckoutService voucherCheckoutService;
    private final TableSessionService tableSessionService;

    @Transactional
    public Order loadForPayment(@NonNull Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional
    public Order prepareForOnlinePayment(@NonNull Long orderId, String voucherCode) {
        Order order = loadForPayment(orderId);
        validatePayable(order);

        if (voucherCode != null && !voucherCode.isBlank()) {
            VoucherPaymentResult voucherResult = voucherCheckoutService.resolveForPayment(
                    voucherCode,
                    currentSubtotalAmount(order));

            order.setVoucherCode(voucherResult.voucherCode());
            orderPricingService.setOrderMoney(
                    order,
                    currentSubtotalAmount(order),
                    voucherResult.appliedDiscountAmount());

            order = orderRepository.save(order);
            orderCacheInvalidationService.evictAfterOrderMutation(order);
            validatePayable(order);
        }

        return order;
    }

    @Transactional
    public Order settleAfterPayment(
            @NonNull Order order,
            @NonNull PaymentMethod paymentMethod,
            User paidBy,
            @NonNull BigDecimal totalPaid,
            @NonNull String auditReason) {

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

        transitionToCompleted(order, fromStatus);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(AppTime.now());
        order.setPaidBy(paidBy);
        order.setPaidAmount(totalPaid);
        order.setBusinessDate(order.getPaymentTime().toLocalDate());

        Order savedOrder = orderRepository.save(order);

        orderAuditService.recordOrderStatus(
                savedOrder,
                fromStatus,
                savedOrder.getStatus(),
                paidBy,
                auditReason);

        orderTableSyncService.recalcTableStatus(savedOrder);
        if (savedOrder.getTableSession() != null) {
            tableSessionService.closeSession(
                    savedOrder.getTableSession().getId(),
                    TableSessionStatus.CLOSED,
                    "Order settled");
        }
        orderCacheInvalidationService.evictAfterOrderMutation(savedOrder);
        eventPublisher.publishEvent(new OrderChangeEvent());

        log.info(
                "Order #{} successfully settled via {} payment",
                savedOrder.getId(),
                paymentMethod);

        return savedOrder;
    }

    private void validatePayable(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(
                    ErrorCode.ORDER_ALREADY_PAID,
                    "This order is already settled");
        }

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID,
                    "Order must be in AWAITING_PAYMENT status before payment. Current: "
                            + order.getStatus());
        }

        if (currentFinalAmount(order).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID,
                    "Order has no payable amount");
        }
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

    private void transitionToCompleted(Order order, OrderStatus fromStatus) {
        try {
            orderStateFactory.validateTransition(fromStatus, OrderStatus.COMPLETED);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE, e.getMessage());
        }

        OrderState state = orderStateFactory.getState(OrderStatus.COMPLETED);
        state.handleRequest(order);
    }
}
