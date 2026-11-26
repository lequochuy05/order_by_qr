package com.qros.modules.order.service;

import com.qros.modules.order.listener.OrderCacheInvalidationService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.promotion.dto.internal.VoucherPaymentResult;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.service.TableSessionService;
import com.qros.modules.user.model.User;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSettlementService {

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = OrderStatus.ACTIVE_STATUSES;

    private final OrderRepository orderRepository;
    private final OrderStateFactory orderStateFactory;
    private final OrderAuditService orderAuditService;
    private final OrderTableSyncService orderTableSyncService;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderPricingService orderPricingService;
    private final VoucherCheckoutService voucherCheckoutService;
    private final TableSessionService tableSessionService;
    private final SystemSettingsService systemSettingsService;

    @Transactional
    public Order loadForPayment(@NonNull Long orderId) {
        return orderRepository
                .findPaymentByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional
    public Order prepareForPayment(@NonNull Long orderId, String voucherCode) {
        return prepareForPayment(orderId, voucherCode, false);
    }

    @Transactional
    public Order prepareForPayment(@NonNull Long orderId, String voucherCode, boolean confirmUnfinishedItems) {
        Order order = loadForPayment(orderId);
        validatePayable(order, confirmUnfinishedItems);

        BigDecimal subtotal = currentSubtotalAmount(order);
        BigDecimal promotionDiscount = safe(orderPricingService.resolveOrderPromotionDiscount(order, subtotal));
        if (promotionDiscount.compareTo(BigDecimal.ZERO) == 0) {
            promotionDiscount = safe(orderPricingService.calculatePromotionDiscount(subtotal));
        }

        boolean voucherChanged = false;
        BigDecimal targetDiscount;

        if (voucherCode != null && !voucherCode.isBlank()) {
            VoucherPaymentResult voucherResult = voucherCheckoutService.resolveForPayment(voucherCode, subtotal);
            targetDiscount = promotionDiscount
                    .add(safe(voucherResult.appliedDiscountAmount()))
                    .min(subtotal);
            order.setVoucherCode(voucherResult.voucherCode());
            voucherChanged = true;
        } else if (voucherCode != null) {
            // Explicit empty string means caller wants to clear an attached voucher.
            targetDiscount = promotionDiscount.min(subtotal);
            if (order.getVoucherCode() != null && !order.getVoucherCode().isBlank()) {
                order.setVoucherCode(null);
                voucherChanged = true;
            }
        } else if (order.getVoucherCode() != null && !order.getVoucherCode().isBlank()) {
            // Null voucherCode preserves and re-validates the voucher already attached to the order.
            VoucherPaymentResult voucherResult =
                    voucherCheckoutService.resolveForPayment(order.getVoucherCode(), subtotal);
            targetDiscount = promotionDiscount
                    .add(safe(voucherResult.appliedDiscountAmount()))
                    .min(subtotal);
            if (!voucherResult.voucherCode().equals(order.getVoucherCode())) {
                order.setVoucherCode(voucherResult.voucherCode());
                voucherChanged = true;
            }
        } else {
            targetDiscount = promotionDiscount.max(currentDiscountAmount(order)).min(subtotal);
        }

        boolean needsRecalc = voucherChanged || targetDiscount.compareTo(currentDiscountAmount(order)) != 0;
        if (!needsRecalc) {
            OrderPricingService.PriceBreakdown breakdown =
                    orderPricingService.calculatePriceBreakdown(subtotal, targetDiscount);
            if (breakdown != null && hasBreakdownChanged(order, breakdown)) {
                needsRecalc = true;
            }
        }

        if (needsRecalc) {
            orderPricingService.setOrderMoney(order, subtotal, targetDiscount);
            order = orderRepository.save(order);
            orderCacheInvalidationService.evictAfterOrderMutation(order);
            validatePayable(order, confirmUnfinishedItems);
        }

        return order;
    }

    @Transactional
    public Order resolveSessionOrderForSelfCheckout(@NonNull String tableCode, @NonNull String sessionToken) {
        TableSession session = tableSessionService.requireOpenSessionForOrdering(tableCode, sessionToken);

        Order activeOrder = orderRepository
                .findFirstByTableSessionIdAndStatusInOrderByCreatedAtDesc(session.getId(), ACTIVE_ORDER_STATUSES)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "No active order found for this table"));

        Order order = loadForPayment(activeOrder.getId());
        boolean hasBillableItems = order.getOrderItems().stream().anyMatch(OrderItem::isBillable);
        if (!hasBillableItems) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "No billable items found for this order");
        }

        boolean allBillableFinished = order.getOrderItems().stream()
                .filter(OrderItem::isBillable)
                .allMatch(item -> item.getStatus() == OrderItemStatus.FINISHED);

        if (!allBillableFinished) {
            throw new BusinessException(
                    ErrorCode.ORDER_HAS_UNFINISHED_ITEMS,
                    "Cannot settle order: kitchen still has items being prepared");
        }

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            OrderStatus fromStatus = order.getStatus();
            orderStateFactory.validateTransition(fromStatus, OrderStatus.AWAITING_PAYMENT);
            orderStateFactory.getState(OrderStatus.AWAITING_PAYMENT).handleRequest(order);
            orderAuditService.recordOrderStatus(order, fromStatus, order.getStatus(), null, "self-checkout-initiated");
            order = orderRepository.save(order);
            orderCacheInvalidationService.evictAfterOrderMutation(order);
        }

        orderTableSyncService.recalcTableStatus(order);
        return order;
    }

    @Transactional(readOnly = true)
    public Order requireActiveSessionOrder(@NonNull String tableCode, @NonNull String sessionToken) {
        TableSession session = tableSessionService.requireOpenSessionForRead(tableCode, sessionToken);
        return orderRepository
                .findFirstByTableSessionIdAndStatusInOrderByCreatedAtDesc(session.getId(), ACTIVE_ORDER_STATUSES)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "No active order found for this table"));
    }

    @Transactional(readOnly = true)
    public boolean belongsToTableCode(Order order, @NonNull String tableCode) {
        if (order == null) {
            return false;
        }
        if (order.getTableSession() != null && order.getTableSession().getId() != null) {
            return tableSessionService.tableEverBelongedToSession(
                    order.getTableSession().getId(), tableCode);
        }
        return order.getTable() != null
                && tableCode.equalsIgnoreCase(order.getTable().getTableCode());
    }

    @Transactional(readOnly = true)
    public boolean verifySelfCheckoutSessionAccess(
            Order order, @NonNull String tableCode, @NonNull String sessionToken) {
        if (order == null
                || order.getTableSession() == null
                || order.getTableSession().getId() == null) {
            return false;
        }
        return tableSessionService.verifySessionAccessForOrder(
                order.getTableSession().getId(), tableCode, sessionToken);
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

        if (order.isCancelled() || order.isCompleted()) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID, "Order cannot be settled in current status: " + order.getStatus());
        }

        OrderStatus fromStatus = order.getStatus();
        boolean hasUnfinishedItems = hasUnfinishedKitchenItems(order);

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(AppTime.now());
        order.setPaidBy(paidBy);
        order.setPaidAmount(totalPaid);
        order.setBusinessDate(order.getPaymentTime().toLocalDate());

        if (hasUnfinishedItems) {
            // Early settlement / prepaid takeaway: keep order active in kitchen until all items finish.
            if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
                order.setStatus(OrderStatus.SERVING);
            }
            Order savedOrder = orderRepository.save(order);
            orderAuditService.recordOrderStatus(
                    savedOrder, fromStatus, savedOrder.getStatus(), paidBy, auditReason + ":early-payment");
            orderCacheInvalidationService.evictAfterOrderMutation(savedOrder);
            eventPublisher.publishEvent(new OrderChangeEvent());
            log.info(
                    "Order #{} settled early via {} payment; keeping status {} while kitchen finishes items",
                    savedOrder.getId(),
                    paymentMethod,
                    savedOrder.getStatus());
            return savedOrder;
        }

        List<String> tableCodes = orderCacheInvalidationService.resolveTableCodes(order);
        transitionToCompleted(order, fromStatus);

        Order savedOrder = orderRepository.save(order);

        orderAuditService.recordOrderStatus(savedOrder, fromStatus, savedOrder.getStatus(), paidBy, auditReason);

        if (savedOrder.getTableSession() != null) {
            tableSessionService.closeSession(
                    savedOrder.getTableSession().getId(), TableSessionStatus.CLOSED, "Order settled");
        } else {
            orderTableSyncService.recalcTableStatus(savedOrder);
        }
        orderCacheInvalidationService.evictAfterOrderMutation(savedOrder, tableCodes);
        eventPublisher.publishEvent(new OrderSettledEvent(savedOrder.getId(), savedOrder.getBusinessDate()));
        eventPublisher.publishEvent(new OrderChangeEvent());

        log.info("Order #{} successfully settled via {} payment", savedOrder.getId(), paymentMethod);

        return savedOrder;
    }

    private void validatePayable(Order order, boolean confirmUnfinishedItems) {
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "This order is already settled");
        }

        if (order.isCancelled()) {
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_INVALID, "Cancelled orders cannot be paid");
        }

        BigDecimal finalAmount = currentFinalAmount(order);
        BigDecimal subtotal = currentSubtotalAmount(order);
        boolean hasBillableItems = order.getOrderItems().stream().anyMatch(OrderItem::isBillable);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_INVALID, "Order has invalid negative payable amount");
        }
        if (finalAmount.compareTo(BigDecimal.ZERO) == 0
                && (subtotal.compareTo(BigDecimal.ZERO) <= 0 || !hasBillableItems)) {
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_INVALID, "Order has no payable amount");
        }

        boolean hasUnfinishedItems = hasUnfinishedKitchenItems(order);

        if (hasUnfinishedItems) {
            boolean allowed = order.isTakeaway()
                    || Boolean.TRUE.equals(
                            systemSettingsService.getSettingsEntity().getAllowSettlementWithUnfinishedItems());
            if (!allowed) {
                throw new BusinessException(
                        ErrorCode.ORDER_HAS_UNFINISHED_ITEMS,
                        "Cannot settle order: kitchen still has items being prepared");
            }
            if (!confirmUnfinishedItems) {
                throw new BusinessException(
                        ErrorCode.ORDER_HAS_UNFINISHED_ITEMS,
                        "Order has items still being prepared. Set confirmUnfinishedItems=true to proceed");
            }
            if (order.getStatus() != OrderStatus.AWAITING_PAYMENT
                    && order.getStatus() != OrderStatus.PENDING
                    && order.getStatus() != OrderStatus.SERVING) {
                throw new BusinessException(
                        ErrorCode.ORDER_PAYMENT_INVALID,
                        "Order cannot be settled in current status: " + order.getStatus());
            }
            log.warn("Order #{} settled with unfinished kitchen items (confirmUnfinishedItems=true)", order.getId());
        } else if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_INVALID,
                    "Order must be in AWAITING_PAYMENT status before payment. Current: " + order.getStatus());
        }
    }

    private boolean hasUnfinishedKitchenItems(Order order) {
        return order.getOrderItems().stream().anyMatch(item -> item.isPending() || item.isCooking());
    }

    private boolean hasBreakdownChanged(Order order, OrderPricingService.PriceBreakdown breakdown) {
        return safe(order.getServiceChargePercentSnapshot()).compareTo(breakdown.serviceChargePercent()) != 0
                || safe(order.getServiceChargeAmount()).compareTo(breakdown.serviceChargeAmount()) != 0
                || safe(order.getTaxPercentSnapshot()).compareTo(breakdown.taxPercent()) != 0
                || safe(order.getTaxAmount()).compareTo(breakdown.taxAmount()) != 0
                || currentFinalAmount(order).compareTo(breakdown.finalAmount()) != 0;
    }

    private BigDecimal currentFinalAmount(Order order) {
        return order.getEffectiveFinalAmount();
    }

    private BigDecimal currentSubtotalAmount(Order order) {
        return order.getSubtotalAmount() != null ? order.getSubtotalAmount() : BigDecimal.ZERO;
    }

    private BigDecimal currentDiscountAmount(Order order) {
        return order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
