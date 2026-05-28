package com.sacmauquan.qrordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    OrderServiceImpl orderService;

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
        when(orderRepository.findFirstByTableIdAndStatusForUpdate(10L, Order.OrderStatus.PENDING))
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
}
