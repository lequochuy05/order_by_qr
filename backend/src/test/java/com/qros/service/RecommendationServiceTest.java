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
import com.qros.modules.recommendation.service.RecommendationService;
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
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.entity.BaseEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qros.modules.menu.dto.MenuItemResponse;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.repository.OrderItemRepository;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    MenuItemRepository menuItemRepository;

    @InjectMocks
    RecommendationService recommendationService;

    @Test
    void personalizedRecommendationsPrioritizeItemsMatchingTimeContext() {
        MenuItem morningDrink = menuItem(1L, "Trà đào", "Trà");
        MenuItem popularSnack = menuItem(2L, "Khoai tây chiên", "Ăn Vặt");

        when(menuItemRepository.findAllByActiveTrue()).thenReturn(List.of(morningDrink, popularSnack));
        when(orderItemRepository.countTotalSoldBatch(List.of(1L, 2L)))
                .thenReturn(List.of(new Object[] { 1L, 0L }, new Object[] { 2L, 50L }));

        List<MenuItemResponse> result = recommendationService.getPersonalizedRecommendations("Sáng", 2);

        assertThat(result).extracting(MenuItemResponse::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    void personalizedRecommendationsUsePopularityForItemsWithSameTimeMatch() {
        MenuItem lessPopularDrink = menuItem(1L, "Trà đào", "Trà");
        MenuItem popularDrink = menuItem(2L, "Soda chanh", "Soda");

        when(menuItemRepository.findAllByActiveTrue()).thenReturn(List.of(lessPopularDrink, popularDrink));
        when(orderItemRepository.countTotalSoldBatch(List.of(1L, 2L)))
                .thenReturn(List.of(new Object[] { 1L, 5L }, new Object[] { 2L, 40L }));

        List<MenuItemResponse> result = recommendationService.getPersonalizedRecommendations("Sáng", 2);

        assertThat(result).extracting(MenuItemResponse::getId)
                .containsExactly(2L, 1L);
    }

    private MenuItem menuItem(Long id, String name, String categoryName) {
        return MenuItem.builder()
                .id(id)
                .name(name)
                .price(BigDecimal.valueOf(30000))
                .active(true)
                .category(Category.builder().id(id.intValue()).name(categoryName).build())
                .build();
    }
}
