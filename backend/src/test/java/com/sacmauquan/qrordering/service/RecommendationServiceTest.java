package com.sacmauquan.qrordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sacmauquan.qrordering.dto.MenuItemResponse;
import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.repository.OrderItemRepository;

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
