package com.qros.modules.kitchen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.kitchen.mapper.KitchenMapper;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderBatch;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.table.model.DiningTable;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    private KitchenMapper kitchenMapper;
    private KitchenService kitchenService;

    @BeforeEach
    void setUp() {
        kitchenMapper = new KitchenMapper();
        kitchenService = new KitchenService(orderRepository, orderService, kitchenMapper);
    }

    @Test
    void getKitchenOrdersReturnsOrdersWithBatchNumbersAndOrderType() {
        DiningTable table = DiningTable.builder().id(1L).tableNumber("B1").build();

        LocalDateTime now = AppTime.now();
        OrderBatch batch1 =
                OrderBatch.builder().id(101L).submittedAt(now.minusMinutes(20)).build();
        OrderBatch batch2 =
                OrderBatch.builder().id(102L).submittedAt(now.minusMinutes(5)).build();

        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.SERVING)
                .orderType(OrderType.DINE_IN)
                .table(table)
                .finalAmount(BigDecimal.valueOf(120000))
                .build();
        order.addBatch(batch1);
        order.addBatch(batch2);

        MenuItem phoBo = MenuItem.builder()
                .id(11L)
                .name("Phở Bò")
                .available(false)
                .category(Category.builder().id(3L).name("Món Nước").build())
                .build();

        Combo traCombo = Combo.builder()
                .id(22L)
                .name("Combo Trà Đá")
                .price(BigDecimal.valueOf(25000))
                .available(true)
                .build();

        OrderItem item1 = OrderItem.builder()
                .id(1L)
                .order(order)
                .batch(batch1)
                .menuItem(phoBo)
                .itemNameSnapshot("Phở Bò")
                .quantity(2)
                .status(OrderItemStatus.PENDING)
                .build();

        OrderItem item2 = OrderItem.builder()
                .id(2L)
                .order(order)
                .batch(batch2)
                .combo(traCombo)
                .itemNameSnapshot("Combo Trà Đá")
                .quantity(1)
                .status(OrderItemStatus.PENDING)
                .build();

        order.addItem(item1);
        order.addItem(item2);

        when(orderRepository.findKitchenOrders(any(), any(), any(), any())).thenReturn(List.of(order));

        List<KitchenOrderResponse> responses = kitchenService.getKitchenOrders();

        assertThat(responses).hasSize(1);
        KitchenOrderResponse response = responses.get(0);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.orderType()).isEqualTo(OrderType.DINE_IN);
        assertThat(response.table().tableNumber()).isEqualTo("B1");
        assertThat(response.orderItems()).hasSize(2);

        KitchenOrderResponse.KitchenOrderItemResponse resItem1 = response.orderItems().stream()
                .filter(i -> i.id().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(resItem1.batchId()).isEqualTo(101L);
        assertThat(resItem1.batchNumber()).isEqualTo(1);
        assertThat(resItem1.menuItem().id()).isEqualTo(11L);
        assertThat(resItem1.menuItem().available()).isFalse();

        KitchenOrderResponse.KitchenOrderItemResponse resItem2 = response.orderItems().stream()
                .filter(i -> i.id().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(resItem2.batchId()).isEqualTo(102L);
        assertThat(resItem2.batchNumber()).isEqualTo(2);
        assertThat(resItem2.combo().id()).isEqualTo(22L);
        assertThat(resItem2.combo().available()).isTrue();
    }

    @Test
    void updateItemStatusDelegatesToOrderService() {
        kitchenService.updateItemStatus(5L, OrderItemStatus.COOKING, 99L);
        verify(orderService).updateItemStatus(5L, OrderItemStatus.COOKING, 99L);
    }

    @Test
    void updateBatchItemStatusUpdatesEachItem() {
        kitchenService.updateBatchItemStatus(List.of(10L, 20L, 30L), OrderItemStatus.COOKING, 99L);
        verify(orderService).updateItemStatus(10L, OrderItemStatus.COOKING, 99L);
        verify(orderService).updateItemStatus(20L, OrderItemStatus.COOKING, 99L);
        verify(orderService).updateItemStatus(30L, OrderItemStatus.COOKING, 99L);
    }

    @Test
    void cancelOrderItemDelegatesToOrderService() {
        kitchenService.cancelOrderItem(15L, "chef@example.com", "Hết nguyên liệu");
        verify(orderService).cancelOrderItem(15L, "chef@example.com", "Hết nguyên liệu");
    }

    @Test
    void markItemPreparedDelegatesToOrderService() {
        kitchenService.markItemPrepared(7L, 42L);
        verify(orderService).markItemPrepared(7L, 42L);
    }
}
