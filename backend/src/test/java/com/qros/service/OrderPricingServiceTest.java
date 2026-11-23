package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderComboRequest;
import com.qros.modules.order.dto.request.OrderItemRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderValidator;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.model.enums.VoucherType;
import com.qros.modules.promotion.service.PromotionService;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
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

@ExtendWith(MockitoExtension.class)
class OrderPricingServiceTest {

    @Mock
    MenuItemRepository menuItemRepository;

    @Mock
    ComboRepository comboRepository;

    @Mock
    ItemOptionValueRepository itemOptionValueRepository;

    @Mock
    VoucherCheckoutService voucherCheckoutService;

    @Mock
    OrderValidator orderValidator;

    @Mock
    TableSessionService tableSessionService;

    @Mock
    PromotionService promotionService;

    private OrderPricingService orderPricingService;

    @BeforeEach
    void setUp() {
        orderPricingService = new OrderPricingService(
                menuItemRepository,
                comboRepository,
                itemOptionValueRepository,
                voucherCheckoutService,
                orderValidator,
                tableSessionService,
                promotionService);
    }

    @Test
    void previewBatchLoadsSelectedOptionValuesOnceForAllItems() {
        MenuItem coffee = menuItem(1L, "Coffee", "10000", optionValue(11L, "Large", "2000"));
        MenuItem tea = menuItem(2L, "Tea", "8000", optionValue(21L, "Honey", "1000"));

        when(menuItemRepository.findAllByIdIn(Set.of(1L, 2L))).thenReturn(List.of(coffee, tea));
        when(itemOptionValueRepository.findAllById(argThat(ids -> idsEqual(ids, Set.of(11L, 21L)))))
                .thenReturn(List.of(firstOptionValue(coffee), firstOptionValue(tea)));
        when(voucherCheckoutService.previewVoucher(isNull(), any(BigDecimal.class)))
                .thenAnswer(invocation -> noDiscount(invocation.getArgument(1)));

        OrderPreviewResponse response = orderPricingService.previewCustomerOrder(new CustomerCreateOrderRequest(
                "T1",
                "SESSION",
                "req-1",
                null,
                List.of(
                        new OrderItemRequest(1L, 2, null, List.of(11L)),
                        new OrderItemRequest(2L, 3, null, List.of(21L)))));

        assertThat(response.subtotalItems()).isEqualByComparingTo("51000");
        verify(tableSessionService).requireOpenSessionForRead("T1", "SESSION");
        verify(itemOptionValueRepository).findAllById(argThat(ids -> idsEqual(ids, Set.of(11L, 21L))));
    }

    @Test
    void previewRejectsInvalidComboQuantity() {
        CustomerCreateOrderRequest request = new CustomerCreateOrderRequest(
                "T1", "SESSION", "req-invalid-combo", List.of(new OrderComboRequest(1L, 0, null)), null);

        assertThatThrownBy(() -> orderPricingService.previewCustomerOrder(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("Combo quantity must be at least 1");
                });
    }

    @Test
    void previewStaffOrderAutomaticallyAppliesPromotionAndStacksVoucher() {
        MenuItem coffee = menuItem(1L, "Coffee", "50000", optionValue(11L, "Standard", "0"));
        when(menuItemRepository.findAllByIdIn(Set.of(1L))).thenReturn(List.of(coffee));
        when(promotionService.calculatePromotionDiscount(new BigDecimal("100000")))
                .thenReturn(new BigDecimal("15000.00"));
        when(voucherCheckoutService.previewVoucher("SAVE10", new BigDecimal("100000")))
                .thenReturn(new DiscountResult(
                        9L,
                        "SAVE10",
                        VoucherType.FIXED_AMOUNT,
                        new BigDecimal("100000"),
                        new BigDecimal("10000.00"),
                        null,
                        new BigDecimal("10000.00"),
                        new BigDecimal("90000.00")));

        StaffCreateOrderRequest request = new StaffCreateOrderRequest(
                1L, null, List.of(new OrderItemRequest(1L, 2, null, List.of())), "SAVE10", OrderType.DINE_IN, null);

        OrderPreviewResponse response = orderPricingService.previewStaffOrder(request);

        assertThat(response.subtotalAmount()).isEqualByComparingTo("100000.00");
        assertThat(response.discountPromotion()).isEqualByComparingTo("15000.00");
        assertThat(response.discountAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.finalAmount()).isEqualByComparingTo("75000.00");
        assertThat(response.voucherValid()).isTrue();
    }

    @Test
    void recalculateOrderTotalsAppliesActivePromotionDiscountAutomatically() {
        Order order = Order.builder().id(1L).build();
        OrderItem item = OrderItem.builder()
                .id(10L)
                .order(order)
                .unitPrice(new BigDecimal("40000"))
                .quantity(2)
                .status(OrderItemStatus.PENDING)
                .build();
        order.getOrderItems().add(item);

        when(promotionService.calculatePromotionDiscount(new BigDecimal("80000")))
                .thenReturn(new BigDecimal("16000.00"));

        orderPricingService.recalculateOrderTotals(order);

        assertThat(order.getSubtotalAmount()).isEqualByComparingTo("80000");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("16000.00");
        assertThat(order.getFinalAmount()).isEqualByComparingTo("64000.00");
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

    private DiscountResult noDiscount(BigDecimal subtotal) {
        return new DiscountResult(
                null, null, null, subtotal, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, subtotal);
    }

    private boolean idsEqual(Iterable<Long> actualIds, Set<Long> expectedIds) {
        Set<Long> actual = StreamSupport.stream(actualIds.spliterator(), false).collect(Collectors.toSet());
        return actual.equals(expectedIds);
    }
}
