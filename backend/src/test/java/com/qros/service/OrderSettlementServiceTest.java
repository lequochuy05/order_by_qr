package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderAuditService;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.order.service.OrderTableSyncService;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.event.DomainEvents.OrderSettledEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderSettlementServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderStateFactory orderStateFactory;

    @Mock
    OrderState completedState;

    @Mock
    OrderAuditService orderAuditService;

    @Mock
    OrderTableSyncService orderTableSyncService;

    @Mock
    OrderCacheInvalidationService orderCacheInvalidationService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    OrderPricingService orderPricingService;

    @Mock
    VoucherCheckoutService voucherCheckoutService;

    @Mock
    TableSessionService tableSessionService;

    @Mock
    SystemSettingsService systemSettingsService;

    private OrderSettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new OrderSettlementService(
                orderRepository,
                orderStateFactory,
                orderAuditService,
                orderTableSyncService,
                orderCacheInvalidationService,
                eventPublisher,
                orderPricingService,
                voucherCheckoutService,
                tableSessionService,
                systemSettingsService);
    }

    @Test
    void settledOrderPublishesBusinessDateForSummaryRefresh() {
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(150_000))
                .discountAmount(BigDecimal.valueOf(20_000))
                .finalAmount(BigDecimal.valueOf(130_000))
                .build();

        when(orderStateFactory.getState(OrderStatus.COMPLETED)).thenReturn(completedState);
        org.mockito.Mockito.doAnswer(invocation -> {
                    order.setStatus(OrderStatus.COMPLETED);
                    return null;
                })
                .when(completedState)
                .handleRequest(order);
        when(orderRepository.save(order)).thenReturn(order);

        settlementService.settleAfterPayment(
                order, PaymentMethod.CASH, null, BigDecimal.valueOf(130_000), "payment-cash");

        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(events.capture());

        List<OrderSettledEvent> settledEvents = events.getAllValues().stream()
                .filter(OrderSettledEvent.class::isInstance)
                .map(OrderSettledEvent.class::cast)
                .toList();

        assertThat(settledEvents).containsExactly(new OrderSettledEvent(10L, order.getBusinessDate()));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("130000");
        assertThat(order.getBusinessDate()).isNotNull();
    }

    @Test
    void alreadyPaidOrderDoesNotPublishAnotherSummaryRefresh() {
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        Order result = settlementService.settleAfterPayment(
                order, PaymentMethod.CASH, null, BigDecimal.valueOf(130_000), "duplicate");

        assertThat(result).isSameAs(order);
        verify(eventPublisher, never()).publishEvent(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void prepareForPaymentThrowsWhenUnfinishedItemsAndSettingDisabled() {
        OrderItem item =
                OrderItem.builder().id(1L).status(OrderItemStatus.COOKING).build();
        Order order = Order.builder()
                .id(20L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .finalAmount(BigDecimal.valueOf(100_000))
                .orderItems(new LinkedHashSet<>(Set.of(item)))
                .build();

        when(orderRepository.findPaymentByIdForUpdate(20L)).thenReturn(Optional.of(order));
        SystemSettings settings = SystemSettings.builder()
                .allowSettlementWithUnfinishedItems(false)
                .build();
        when(systemSettingsService.getSettingsEntity()).thenReturn(settings);

        assertThatThrownBy(() -> settlementService.prepareForPayment(20L, null, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_HAS_UNFINISHED_ITEMS);
    }

    @Test
    void prepareForPaymentThrowsWhenUnfinishedItemsSettingEnabledButUnconfirmed() {
        OrderItem item =
                OrderItem.builder().id(1L).status(OrderItemStatus.PENDING).build();
        Order order = Order.builder()
                .id(21L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .finalAmount(BigDecimal.valueOf(100_000))
                .orderItems(new LinkedHashSet<>(Set.of(item)))
                .build();

        when(orderRepository.findPaymentByIdForUpdate(21L)).thenReturn(Optional.of(order));
        SystemSettings settings = SystemSettings.builder()
                .allowSettlementWithUnfinishedItems(true)
                .build();
        when(systemSettingsService.getSettingsEntity()).thenReturn(settings);

        assertThatThrownBy(() -> settlementService.prepareForPayment(21L, null, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_HAS_UNFINISHED_ITEMS);
    }

    @Test
    void prepareForPaymentSucceedsWhenUnfinishedItemsSettingEnabledAndConfirmed() {
        OrderItem item =
                OrderItem.builder().id(1L).status(OrderItemStatus.PENDING).build();
        Order order = Order.builder()
                .id(22L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .finalAmount(BigDecimal.valueOf(100_000))
                .orderItems(new LinkedHashSet<>(Set.of(item)))
                .build();

        when(orderRepository.findPaymentByIdForUpdate(22L)).thenReturn(Optional.of(order));
        SystemSettings settings = SystemSettings.builder()
                .allowSettlementWithUnfinishedItems(true)
                .build();
        when(systemSettingsService.getSettingsEntity()).thenReturn(settings);

        Order result = settlementService.prepareForPayment(22L, null, true);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(22L);
    }
}
