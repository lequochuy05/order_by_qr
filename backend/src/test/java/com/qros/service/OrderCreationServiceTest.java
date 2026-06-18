package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.inventory.service.InventoryReservationService;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderItemRequest;
import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderCreationService;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderStatusService;
import com.qros.modules.order.service.OrderTableSyncService;
import com.qros.modules.order.service.OrderValidator;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    MenuItemRepository menuItemRepository;

    @Mock
    DiningTableRepository tableRepository;

    @Mock
    ComboRepository comboRepository;

    @Mock
    ItemOptionValueRepository itemOptionValueRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    OrderPricingService orderPricingService;

    @Mock
    OrderStatusService orderStatusService;

    @Mock
    OrderTableSyncService orderTableSyncService;

    @Mock
    OrderCacheInvalidationService orderCacheInvalidationService;

    @Mock
    InventoryReservationService inventoryReservationService;

    @Mock
    OrderValidator orderValidator;

    @Mock
    TableSessionService tableSessionService;

    @Mock
    PaymentTransactionRepository paymentTransactionRepository;

    private OrderCreationService orderCreationService;

    @BeforeEach
    void setUp() {
        orderCreationService = new OrderCreationService(
                orderRepository,
                menuItemRepository,
                tableRepository,
                comboRepository,
                itemOptionValueRepository,
                eventPublisher,
                orderPricingService,
                orderStatusService,
                orderTableSyncService,
                new OrderMapper(),
                new SimpleMeterRegistry(),
                orderCacheInvalidationService,
                inventoryReservationService,
                orderValidator,
                tableSessionService,
                paymentTransactionRepository);
        orderCreationService.initCounters();
    }

    @Test
    void createOrderBatchLoadsSelectedOptionValuesOnceForAllItems() {
        DiningTable table = DiningTable.builder()
                .id(7L)
                .tableNumber("A1")
                .tableCode("T1")
                .status(TableStatus.AVAILABLE)
                .capacity(4)
                .build();
        TableSession session = TableSession.builder().id(77L).table(table).build();
        MenuItem coffee = menuItem(1L, "Coffee", "10000", optionValue(11L, "Large", "2000"));
        MenuItem tea = menuItem(2L, "Tea", "8000", optionValue(21L, "Honey", "1000"));

        when(tableSessionService.requireOpenSessionForOrdering("T1", "SESSION")).thenReturn(session);
        when(orderRepository.findActiveByTableSessionIdForUpdate(any(), any())).thenReturn(List.of());
        when(orderRepository.findActiveByTableIdForUpdate(any(), any())).thenReturn(List.of());
        when(menuItemRepository.findAllByIdIn(Set.of(1L, 2L))).thenReturn(List.of(coffee, tea));
        when(itemOptionValueRepository.findAllByIdIn(argThat(ids -> idsEqual(ids, Set.of(11L, 21L)))))
                .thenReturn(List.of(firstOptionValue(coffee), firstOptionValue(tea)));
        when(orderPricingService.calculateLineTotal(any(BigDecimal.class), anyInt()))
                .thenAnswer(invocation -> invocation
                        .<BigDecimal>getArgument(0)
                        .multiply(BigDecimal.valueOf(invocation.<Integer>getArgument(1))));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(paymentTransactionRepository.findPendingOnlineTransactionsByOrderId(100L))
                .thenReturn(List.of());

        orderCreationService.createCustomerOrder(new CustomerCreateOrderRequest(
                "T1",
                "SESSION",
                "req-1",
                null,
                List.of(
                        new OrderItemRequest(1L, 2, null, List.of(11L)),
                        new OrderItemRequest(2L, 3, null, List.of(21L)))));

        verify(itemOptionValueRepository).findAllByIdIn(argThat(ids -> idsEqual(ids, Set.of(11L, 21L))));
    }

    @Test
    void createCustomerOrderRejectsWhenPendingPaymentExists() {
        DiningTable table = DiningTable.builder()
                .id(7L)
                .tableNumber("A1")
                .tableCode("T1")
                .status(TableStatus.OCCUPIED)
                .capacity(4)
                .build();
        TableSession session = TableSession.builder().id(77L).table(table).build();
        Order activeOrder =
                Order.builder().id(100L).table(table).tableSession(session).build();

        when(tableSessionService.requireOpenSessionForOrdering("T1", "SESSION")).thenReturn(session);
        when(orderRepository.findActiveByTableSessionIdForUpdate(any(), any())).thenReturn(List.of(activeOrder));
        when(paymentTransactionRepository.existsByOrderIdAndStatus(100L, PaymentTransactionStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> orderCreationService.createCustomerOrder(new CustomerCreateOrderRequest(
                        "T1", "SESSION", "req-2", null, List.of(new OrderItemRequest(1L, 1, null, List.of())))))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_PAYMENT_IN_PROGRESS));

        verify(orderRepository, never()).save(any(Order.class));
    }

    private MenuItem menuItem(Long id, String name, String price, ItemOptionValue value) {
        Category category =
                Category.builder().id(1L).name("Drinks").active(true).build();

        MenuItem item = MenuItem.builder()
                .id(id)
                .name(name)
                .price(new BigDecimal(price))
                .active(true)
                .available(true)
                .category(category)
                .build();

        ItemOption option = ItemOption.builder()
                .id(id * 10)
                .name("Option")
                .required(false)
                .maxSelection(1)
                .menuItem(item)
                .build();

        value.setItemOption(option);
        option.getOptionValues().add(value);
        item.getItemOptions().add(option);

        return item;
    }

    private ItemOptionValue optionValue(Long id, String name, String extraPrice) {
        return ItemOptionValue.builder()
                .id(id)
                .name(name)
                .extraPrice(new BigDecimal(extraPrice))
                .build();
    }

    private ItemOptionValue firstOptionValue(MenuItem item) {
        return item.getItemOptions()
                .iterator()
                .next()
                .getOptionValues()
                .iterator()
                .next();
    }

    private boolean idsEqual(Iterable<Long> actualIds, Set<Long> expectedIds) {
        Set<Long> actual = StreamSupport.stream(actualIds.spliterator(), false).collect(Collectors.toSet());
        return actual.equals(expectedIds);
    }
}
