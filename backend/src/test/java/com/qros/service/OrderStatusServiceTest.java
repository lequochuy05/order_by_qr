package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.inventory.service.InventoryReservationService;
import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderAuditService;
import com.qros.modules.order.service.OrderStatusService;
import com.qros.modules.order.service.OrderTableSyncService;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.service.TableSessionService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderStatusServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderStateFactory orderStateFactory;

    @Mock
    OrderAuditService orderAuditService;

    @Mock
    OrderTableSyncService orderTableSyncService;

    @Mock
    OrderCacheInvalidationService orderCacheInvalidationService;

    @Mock
    InventoryReservationService inventoryReservationService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    TableSessionService tableSessionService;

    @Mock
    OrderState servingState;

    private OrderStatusService orderStatusService;

    @BeforeEach
    void setUp() {
        orderStatusService = new OrderStatusService(
                orderRepository,
                orderStateFactory,
                orderAuditService,
                orderTableSyncService,
                orderCacheInvalidationService,
                inventoryReservationService,
                eventPublisher,
                new OrderMapper(),
                tableSessionService);
    }

    @Test
    void updateStatusDoesNotCloseOpenTableSession() {
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .tableSession(TableSession.builder().id(77L).build())
                .build();

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(orderStateFactory.getState(OrderStatus.SERVING)).thenReturn(servingState);
        doAnswer(invocation -> {
                    Order target = invocation.getArgument(0);
                    target.setStatus(OrderStatus.SERVING);
                    return null;
                })
                .when(servingState)
                .handleRequest(any(Order.class));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderStatusService.updateStatus(10L, OrderStatus.SERVING);

        verify(tableSessionService, never()).closeSession(any(), any(), any());
    }

    @Test
    void cancelOrderClosesTableSessionAsCancelled() {
        Order order = Order.builder()
                .id(11L)
                .status(OrderStatus.SERVING)
                .paymentStatus(PaymentStatus.PENDING)
                .tableSession(TableSession.builder().id(88L).build())
                .build();

        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderStatusService.cancelOrder(11L);

        verify(tableSessionService).closeSession(eq(88L), eq(TableSessionStatus.CANCELLED), eq("Order cancelled"));
    }

    @Test
    void addingPendingItemReopensAwaitingPaymentOrderToServing() {
        Order order = Order.builder()
                .id(12L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        order.addItem(
                OrderItem.builder().id(101L).status(OrderItemStatus.FINISHED).build());
        order.addItem(
                OrderItem.builder().id(102L).status(OrderItemStatus.PENDING).build());

        when(orderStateFactory.getState(OrderStatus.SERVING)).thenReturn(servingState);
        doAnswer(invocation -> {
                    Order target = invocation.getArgument(0);
                    target.setStatus(OrderStatus.SERVING);
                    return null;
                })
                .when(servingState)
                .handleRequest(order);
        when(orderRepository.save(order)).thenReturn(order);

        orderStatusService.tryAutoPromoteOrder(order);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SERVING);
        verify(orderStateFactory).validateTransition(OrderStatus.AWAITING_PAYMENT, OrderStatus.SERVING);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderRestoresOnlyReservableItems() {
        Order order = Order.builder()
                .id(13L)
                .status(OrderStatus.SERVING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderItem pending =
                OrderItem.builder().id(201L).status(OrderItemStatus.PENDING).build();
        OrderItem cooking =
                OrderItem.builder().id(202L).status(OrderItemStatus.COOKING).build();
        OrderItem finished =
                OrderItem.builder().id(203L).status(OrderItemStatus.FINISHED).build();
        OrderItem cancelled =
                OrderItem.builder().id(204L).status(OrderItemStatus.CANCELLED).build();
        order.addItem(pending);
        order.addItem(cooking);
        order.addItem(finished);
        order.addItem(cancelled);

        when(orderRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderStatusService.cancelOrder(13L);

        verify(inventoryReservationService).restoreOrderItem(pending);
        verify(inventoryReservationService).restoreOrderItem(cooking);
        verify(inventoryReservationService, never()).restoreOrderItem(finished);
        verify(inventoryReservationService, never()).restoreOrderItem(cancelled);
    }

    @Test
    void deleteOrderRestoresOnlyReservableItems() {
        Order order = Order.builder()
                .id(14L)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderItem pending =
                OrderItem.builder().id(301L).status(OrderItemStatus.PENDING).build();
        OrderItem finished =
                OrderItem.builder().id(302L).status(OrderItemStatus.FINISHED).build();
        order.addItem(pending);
        order.addItem(finished);

        when(orderRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(order));

        orderStatusService.deleteOrder(14L);

        verify(inventoryReservationService).restoreOrderItem(pending);
        verify(inventoryReservationService, never()).restoreOrderItem(finished);
        verify(orderRepository).delete(order);
    }
}
