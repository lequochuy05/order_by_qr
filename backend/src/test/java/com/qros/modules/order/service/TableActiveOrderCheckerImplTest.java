package com.qros.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableActiveOrderCheckerImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderStateFactory orderStateFactory;

    @Mock
    private OrderAuditService orderAuditService;

    @Mock
    private OrderCacheInvalidationService orderCacheInvalidationService;

    private TableActiveOrderCheckerImpl checker;

    @BeforeEach
    void setUp() {
        checker = new TableActiveOrderCheckerImpl(
                orderRepository,
                paymentTransactionRepository,
                orderStateFactory,
                orderAuditService,
                orderCacheInvalidationService);
    }

    @Test
    void validateAndPrepareSessionForPaymentRequestRejectsWhenUnfinishedItemsExist() {
        OrderItem pending =
                OrderItem.builder().id(1L).status(OrderItemStatus.PENDING).build();
        OrderItem cooking =
                OrderItem.builder().id(2L).status(OrderItemStatus.COOKING).build();
        OrderItem finished =
                OrderItem.builder().id(3L).status(OrderItemStatus.FINISHED).build();
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.SERVING)
                .orderItems(new LinkedHashSet<>(Set.of(pending, cooking, finished)))
                .build();

        when(orderRepository.findActiveByTableSessionIdForUpdate(eq100(), any()))
                .thenReturn(List.of(order));

        assertThatThrownBy(() -> checker.validateAndPrepareSessionForPaymentRequest(100L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_HAS_UNFINISHED_ITEMS));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SERVING);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void validateAndPrepareSessionForPaymentRequestRejectsWhenAllItemsCancelled() {
        OrderItem cancelled =
                OrderItem.builder().id(1L).status(OrderItemStatus.CANCELLED).build();
        Order order = Order.builder()
                .id(11L)
                .status(OrderStatus.SERVING)
                .orderItems(new LinkedHashSet<>(Set.of(cancelled)))
                .build();

        when(orderRepository.findActiveByTableSessionIdForUpdate(eq100(), any()))
                .thenReturn(List.of(order));

        assertThatThrownBy(() -> checker.validateAndPrepareSessionForPaymentRequest(100L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void validateAndPrepareSessionForPaymentRequestTransitionsOrderWhenAllBillableItemsFinished() {
        OrderItem finished =
                OrderItem.builder().id(1L).status(OrderItemStatus.FINISHED).build();
        OrderItem cancelled =
                OrderItem.builder().id(2L).status(OrderItemStatus.CANCELLED).build();
        Order order = Order.builder()
                .id(12L)
                .status(OrderStatus.SERVING)
                .orderItems(new LinkedHashSet<>(Set.of(finished, cancelled)))
                .build();

        OrderState awaitingState = mock(OrderState.class);
        org.mockito.Mockito.doAnswer(invocation -> {
                    order.setStatus(OrderStatus.AWAITING_PAYMENT);
                    return null;
                })
                .when(awaitingState)
                .handleRequest(order);

        when(orderRepository.findActiveByTableSessionIdForUpdate(eq100(), any()))
                .thenReturn(List.of(order));
        when(orderStateFactory.getState(OrderStatus.AWAITING_PAYMENT)).thenReturn(awaitingState);
        when(orderRepository.save(order)).thenReturn(order);

        checker.validateAndPrepareSessionForPaymentRequest(100L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verify(orderStateFactory).validateTransition(OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT);
        verify(orderRepository).save(order);
    }

    private static Long eq100() {
        return org.mockito.ArgumentMatchers.eq(100L);
    }
}
