package com.qros.test;

import com.qros.modules.menu.service.CategoryService;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.modules.menu.service.ComboService;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.order.service.impl.OrderServiceImpl;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.modules.payment.service.PayosService;
import com.qros.modules.payment.service.impl.PayosServiceImpl;
import com.qros.modules.promotion.service.DiscountService;
import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.modules.recomendation.service.RecommendationService;
import com.qros.modules.user.service.UserService;
import com.qros.modules.table.service.DiningTableService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.OrderItemOption;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.user.model.User;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.order.dto.OrderRequest;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.payment.dto.PayosCreateRequest;
import com.qros.modules.payment.dto.PayosCreateResponse;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.entity.BaseEntity;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.qros.modules.order.dto.OrderRequest;
import com.qros.modules.menu.model.Category;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderCreationService;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderQueryService;
import com.qros.modules.order.service.OrderStatusService;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.user.repository.UserRepository;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.shared.util.AppTime;

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
    @Mock
    OrderStatusService orderStatusService;
    @Spy
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    OrderCreationService orderCreationService;
    KitchenService kitchenService;

    @BeforeEach
    void setUp() {
        OrderMapper orderMapper = new OrderMapper();
        OrderPricingService orderPricingService = new OrderPricingService(
                menuItemRepository,
                comboRepository,
                itemOptionValueRepository,
                discountService);
        orderCreationService = new OrderCreationService(
                orderRepository,
                menuItemRepository,
                tableRepository,
                comboRepository,
                itemOptionValueRepository,
                notificationService,
                orderPricingService,
                orderStatusService,
                orderMapper,
                meterRegistry);
        orderCreationService.initCounters();
        kitchenService = new KitchenService(orderRepository, orderStatusService);
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

        orderCreationService.createOrder(request);

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
        Cacheable cacheable = OrderQueryService.class
                .getMethod("getOrderStats", String.class, java.time.LocalDate.class, java.time.LocalDate.class,
                        String.class, String.class)
                .getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.value()).containsExactly("order_stats");
        assertThat(cacheable.key()).isEmpty();
    }

    @Test
    void orderHistoryFetchesPageIdsBeforeLoadingDetails() {
        OrderQueryService queryService = new OrderQueryService(
                orderRepository,
                transactionRepository,
                payosService,
                new OrderMapper());
        PageRequest pageable = PageRequest.of(0, 15);
        Order pageOrderA = Order.builder().id(2L).build();
        Order pageOrderB = Order.builder().id(1L).build();
        Order detailOrderA = historyOrder(2L, "A2");
        Order detailOrderB = historyOrder(1L, "A1");

        when(orderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Order>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pageOrderA, pageOrderB), pageable, 2));
        when(orderRepository.findDistinctByIdIn(List.of(2L, 1L)))
                .thenReturn(List.of(detailOrderB, detailOrderA));

        var history = queryService.getOrderHistory("COMPLETED", null, null, null, null, pageable);

        assertThat(history.getTotalElements()).isEqualTo(2);
        assertThat(history.getContent())
                .extracting(OrderResponse::id)
                .containsExactly(2L, 1L);
    }

    @Test
    void kitchenOrdersIncludeRecentlyFinishedItemsButExcludeOldFinishedOnlyOrders() {
        Order recentFinishedOrder = kitchenOrder(1L, OrderItem.OrderItemStatus.FINISHED, AppTime.now().minusMinutes(5));
        Order oldFinishedOrder = kitchenOrder(2L, OrderItem.OrderItemStatus.FINISHED, AppTime.now().minusMinutes(20));
        Order pendingOrder = kitchenOrder(3L, OrderItem.OrderItemStatus.PENDING, AppTime.now().minusMinutes(30));

        when(orderRepository.findByStatusIn(List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING, Order.OrderStatus.AWAITING_PAYMENT)))
                .thenReturn(List.of(oldFinishedOrder, recentFinishedOrder, pendingOrder));

        assertThat(kitchenService.getKitchenOrders())
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

    private Order historyOrder(Long id, String tableNumber) {
        DiningTable table = DiningTable.builder()
                .id(id)
                .tableNumber(tableNumber)
                .tableCode("table-" + id)
                .capacity(4)
                .build();

        Order order = Order.builder()
                .id(id)
                .table(table)
                .status(Order.OrderStatus.COMPLETED)
                .paymentStatus(Order.PaymentStatus.PAID)
                .paymentMethod(Order.PaymentMethod.CASH)
                .orderType(Order.OrderType.DINE_IN)
                .originalTotal(BigDecimal.valueOf(100000))
                .discountVoucher(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100000))
                .createdAt(AppTime.now())
                .build();

        order.getOrderItems().add(OrderItem.builder()
                .id(id * 10)
                .order(order)
                .unitPrice(BigDecimal.valueOf(100000))
                .quantity(1)
                .status(OrderItem.OrderItemStatus.FINISHED)
                .prepared(true)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getCreatedAt())
                .build());

        return order;
    }
}
