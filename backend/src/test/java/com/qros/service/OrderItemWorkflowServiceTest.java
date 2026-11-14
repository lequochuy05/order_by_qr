package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderAuditService;
import com.qros.modules.order.service.OrderItemWorkflowService;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderStatusService;
import com.qros.modules.order.service.OrderTableSyncService;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.event.DomainEvents.OrderChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderItemWorkflowServiceTest {

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    InventoryReservationService inventoryReservationService;

    @Mock
    OrderPricingService orderPricingService;

    @Mock
    OrderAuditService orderAuditService;

    @Mock
    OrderStatusService orderStatusService;

    @Mock
    OrderTableSyncService orderTableSyncService;

    @Mock
    OrderCacheInvalidationService orderCacheInvalidationService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    OrderMapper orderMapper;

    private OrderItemWorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new OrderItemWorkflowService(
                orderItemRepository,
                orderRepository,
                userRepository,
                inventoryReservationService,
                orderPricingService,
                orderAuditService,
                orderStatusService,
                orderTableSyncService,
                orderCacheInvalidationService,
                eventPublisher,
                orderMapper);
    }

    @Test
    void cancelOrderItemAllowsCookingItemAndRestoresInventory() {
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.SERVING)
                .subtotalAmount(BigDecimal.valueOf(50000))
                .finalAmount(BigDecimal.valueOf(50000))
                .build();

        OrderItem cookingItem = OrderItem.builder()
                .id(10L)
                .order(order)
                .status(OrderItemStatus.COOKING)
                .quantity(1)
                .lineTotal(BigDecimal.valueOf(50000))
                .build();
        order.getOrderItems().add(cookingItem);

        when(orderItemRepository.findDetailByIdForUpdate(10L)).thenReturn(Optional.of(cookingItem));
        when(orderRepository.findWorkflowByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workflowService.cancelOrderItem(10L, "chef@example.com", "Burnt dish in kitchen");

        assertThat(cookingItem.getStatus()).isEqualTo(OrderItemStatus.CANCELLED);
        verify(inventoryReservationService).restoreOrderItem(cookingItem);
        verify(orderPricingService).recalculateOrderTotals(order);
        verify(eventPublisher).publishEvent(any(OrderChangeEvent.class));
    }

    @Test
    void updateItemStatusAllowsTransitionFromCookingToCancelled() {
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.SERVING)
                .subtotalAmount(BigDecimal.valueOf(50000))
                .finalAmount(BigDecimal.valueOf(50000))
                .build();

        OrderItem cookingItem = OrderItem.builder()
                .id(10L)
                .order(order)
                .status(OrderItemStatus.COOKING)
                .quantity(1)
                .lineTotal(BigDecimal.valueOf(50000))
                .build();
        order.getOrderItems().add(cookingItem);

        when(orderItemRepository.findDetailByIdForUpdate(10L)).thenReturn(Optional.of(cookingItem));
        when(orderRepository.findWorkflowByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workflowService.updateItemStatus(10L, OrderItemStatus.CANCELLED, null);

        assertThat(cookingItem.getStatus()).isEqualTo(OrderItemStatus.CANCELLED);
        verify(inventoryReservationService).restoreOrderItem(cookingItem);
        verify(orderPricingService).recalculateOrderTotals(order);
        verify(eventPublisher).publishEvent(any(OrderChangeEvent.class));
    }

    @Test
    void updateItemStatusConsumesInventoryWhenTransitioningToFinished() {
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.SERVING)
                .subtotalAmount(BigDecimal.valueOf(50000))
                .finalAmount(BigDecimal.valueOf(50000))
                .build();

        OrderItem cookingItem = OrderItem.builder()
                .id(10L)
                .order(order)
                .status(OrderItemStatus.COOKING)
                .quantity(1)
                .lineTotal(BigDecimal.valueOf(50000))
                .build();
        order.getOrderItems().add(cookingItem);

        when(orderItemRepository.findDetailByIdForUpdate(10L)).thenReturn(Optional.of(cookingItem));
        when(orderRepository.findWorkflowByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workflowService.updateItemStatus(10L, OrderItemStatus.FINISHED, null);

        assertThat(cookingItem.getStatus()).isEqualTo(OrderItemStatus.FINISHED);
        verify(inventoryReservationService).consumeForOrderItem(cookingItem);
        verify(inventoryReservationService, never()).restoreOrderItem(any());
    }

    @Test
    void updateItemStatusRejectsTransitionFromFinished() {
        Order order = Order.builder().id(1L).status(OrderStatus.SERVING).build();

        OrderItem finishedItem = OrderItem.builder()
                .id(10L)
                .order(order)
                .status(OrderItemStatus.FINISHED)
                .build();
        order.getOrderItems().add(finishedItem);

        when(orderItemRepository.findDetailByIdForUpdate(10L)).thenReturn(Optional.of(finishedItem));

        assertThatThrownBy(() -> workflowService.updateItemStatus(10L, OrderItemStatus.CANCELLED, null))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_INVALID_ITEM_STATUS));

        verify(inventoryReservationService, never()).restoreOrderItem(any());
        verify(orderRepository, never()).save(any());
    }
}
