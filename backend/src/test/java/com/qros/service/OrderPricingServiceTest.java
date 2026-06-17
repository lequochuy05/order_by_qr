package com.qros.service;

import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderItemRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderValidator;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.table.service.TableSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private OrderPricingService orderPricingService;

    @BeforeEach
    void setUp() {
        orderPricingService = new OrderPricingService(
                menuItemRepository,
                comboRepository,
                itemOptionValueRepository,
                voucherCheckoutService,
                orderValidator,
                tableSessionService);
    }

    @Test
    void previewBatchLoadsSelectedOptionValuesOnceForAllItems() {
        MenuItem coffee = menuItem(1L, "Coffee", "10000", optionValue(11L, "Large", "2000"));
        MenuItem tea = menuItem(2L, "Tea", "8000", optionValue(21L, "Honey", "1000"));

        when(menuItemRepository.findAllByIdIn(Set.of(1L, 2L))).thenReturn(List.of(coffee, tea));
        when(itemOptionValueRepository.findAllById(argThat(ids -> idsEqual(ids, Set.of(11L, 21L)))))
                .thenReturn(List.of(
                        firstOptionValue(coffee),
                        firstOptionValue(tea)));
        when(voucherCheckoutService.previewVoucher(isNull(), any(BigDecimal.class)))
                .thenAnswer(invocation -> noDiscount(invocation.getArgument(1)));

        OrderPreviewResponse response = orderPricingService.previewCustomerOrder(
                new CustomerCreateOrderRequest(
                        "T1",
                        "SESSION",
                        null,
                        List.of(
                                new OrderItemRequest(1L, 2, null, List.of(11L)),
                                new OrderItemRequest(2L, 3, null, List.of(21L)))));

        assertThat(response.subtotalItems()).isEqualByComparingTo("51000");
        verify(tableSessionService).requireOpenSessionForRead("T1", "SESSION");
        verify(itemOptionValueRepository).findAllById(argThat(ids -> idsEqual(ids, Set.of(11L, 21L))));
    }

    private MenuItem menuItem(Long id, String name, String price, ItemOptionValue value) {
        Category category = Category.builder()
                .id(1L)
                .name("Drinks")
                .active(true)
                .build();

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
        return item.getItemOptions().iterator().next().getOptionValues().iterator().next();
    }

    private DiscountResult noDiscount(BigDecimal subtotal) {
        return new DiscountResult(
                null,
                null,
                null,
                subtotal,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                subtotal);
    }

    private boolean idsEqual(Iterable<Long> actualIds, Set<Long> expectedIds) {
        Set<Long> actual = StreamSupport.stream(actualIds.spliterator(), false)
                .collect(Collectors.toSet());
        return actual.equals(expectedIds);
    }
}
