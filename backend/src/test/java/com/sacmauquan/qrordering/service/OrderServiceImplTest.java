package com.sacmauquan.qrordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.OrderItem;
import com.sacmauquan.qrordering.repository.ComboRepository;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import com.sacmauquan.qrordering.repository.ItemOptionValueRepository;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.repository.OrderItemRepository;
import com.sacmauquan.qrordering.repository.OrderRepository;
import com.sacmauquan.qrordering.repository.PaymentTransactionRepository;
import com.sacmauquan.qrordering.repository.UserRepository;
import com.sacmauquan.qrordering.service.impl.OrderServiceImpl;
import com.sacmauquan.qrordering.state.OrderStateFactory;
import com.sacmauquan.qrordering.util.AppTime;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    OrderRepository orderRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    OrderItemRepository orderItemRepository;
    @Mock
    MenuItemRepository menuItemRepository;
    @Mock
    DiningTableRepository tableRepository;
    @Mock
    ComboRepository comboRepository;
    @Mock
    ItemOptionValueRepository itemOptionValueRepository;
    @Mock
    DiscountService discountService;
    @Mock
    PaymentTransactionRepository transactionRepository;
    @Mock
    PayosService payosService;
    @Mock
    OrderStateFactory orderStateFactory;
    @Mock
    NotificationService notificationService;
    @Spy
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService.initCounters();
    }

    @Test
    void createOrderMergesMatchingUnpreparedItemForSameTable() {
        DiningTable table = DiningTable.builder()
                .id(10L)
                .tableNumber("A1")
                .tableCode("table-code")
                .capacity(4)
                .status(DiningTable.TableStatus.AVAILABLE)
                .build();
        Category category = Category.builder().id(1).name("Drinks").build();
        MenuItem item = MenuItem.builder()
                .id(20L)
                .name("Coffee")
                .price(BigDecimal.valueOf(30000))
                .category(category)
                .build();
        Order existingOrder = Order.builder()
                .id(30L)
                .table(table)
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderType(Order.OrderType.DINE_IN)
                .originalTotal(BigDecimal.valueOf(60000))
                .discountVoucher(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(60000))
                .build();
        existingOrder.getOrderItems().add(OrderItem.builder()
                .id(40L)
                .order(existingOrder)
                .menuItem(item)
                .unitPrice(BigDecimal.valueOf(30000))
                .quantity(2)
                .notes("less sugar")
                .status(OrderItem.OrderItemStatus.PENDING)
                .prepared(false)
                .build());

        OrderRequest request = new OrderRequest();
        request.setTableCode("table-code");
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setMenuItemId(20L);
        itemRequest.setQuantity(3);
        itemRequest.setNotes("less sugar");
        request.setItems(java.util.List.of(itemRequest));

        when(tableRepository.findByTableCode("table-code")).thenReturn(Optional.of(table));
        when(orderRepository.findFirstByTableIdAndStatusInForUpdate(10L, 
                java.util.List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING, Order.OrderStatus.AWAITING_PAYMENT)))
                .thenReturn(Optional.of(existingOrder));
        when(menuItemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        OrderItem savedItem = saved.getOrderItems().iterator().next();

        assertThat(saved.getOrderItems()).hasSize(1);
        assertThat(savedItem.getQuantity()).isEqualTo(5);
        assertThat(saved.getOriginalTotal()).isEqualByComparingTo("150000");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("150000");
        assertThat(table.getStatus()).isEqualTo(DiningTable.TableStatus.OCCUPIED);
    }

    @Test
    void orderStatsUsesNullSafeDefaultCacheKey() throws Exception {
        Cacheable cacheable = OrderServiceImpl.class
                .getMethod("getOrderStats", String.class, java.time.LocalDate.class, java.time.LocalDate.class,
                        String.class, String.class)
                .getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.value()).containsExactly("order_stats");
        assertThat(cacheable.key()).isEmpty();
    }

    @Test
    void kitchenOrdersIncludeRecentlyFinishedItemsButExcludeOldFinishedOnlyOrders() {
        Order recentFinishedOrder = kitchenOrder(1L, OrderItem.OrderItemStatus.FINISHED, AppTime.now().minusMinutes(5));
        Order oldFinishedOrder = kitchenOrder(2L, OrderItem.OrderItemStatus.FINISHED, AppTime.now().minusMinutes(20));
        Order pendingOrder = kitchenOrder(3L, OrderItem.OrderItemStatus.PENDING, AppTime.now().minusMinutes(30));

        when(orderRepository.findByStatusIn(List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING, Order.OrderStatus.AWAITING_PAYMENT)))
                .thenReturn(List.of(oldFinishedOrder, recentFinishedOrder, pendingOrder));

        assertThat(orderService.getKitchenOrders())
                .extracting(response -> response.id())
                .containsExactly(1L, 3L);
    }

    private Order kitchenOrder(Long id, OrderItem.OrderItemStatus itemStatus, LocalDateTime itemUpdatedAt) {
        Order order = Order.builder()
                .id(id)
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderType(Order.OrderType.DINE_IN)
                .originalTotal(BigDecimal.ZERO)
                .discountVoucher(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(AppTime.now().minusMinutes(40 - id))
                .build();

        order.getOrderItems().add(OrderItem.builder()
                .id(id * 10)
                .order(order)
                .unitPrice(BigDecimal.ZERO)
                .quantity(1)
                .status(itemStatus)
                .prepared(itemStatus == OrderItem.OrderItemStatus.FINISHED)
                .createdAt(order.getCreatedAt())
                .updatedAt(itemUpdatedAt)
                .build());

        return order;
    }
}
