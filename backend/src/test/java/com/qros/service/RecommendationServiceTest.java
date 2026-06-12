package com.qros.service;

import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.recommendation.dto.response.RecommendationItemResponse;
import com.qros.modules.recommendation.dto.response.RecommendationResponse;
import com.qros.modules.recommendation.mapper.RecommendationMapper;
import com.qros.modules.recommendation.model.enums.RecommendationContext;
import com.qros.modules.recommendation.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    MenuItemRepository menuItemRepository;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                menuItemRepository,
                orderItemRepository,
                new RecommendationMapper());
    }

    @Test
    void personalizedRecommendationsUseCurrentContextCandidates() {
        MenuItem morningDrink = menuItem(1L, "Trà đào", "Trà sữa");
        MenuItem snack = menuItem(2L, "Khoai tây chiên", "Ăn vặt");

        when(menuItemRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(morningDrink, snack));

        RecommendationResponse response = recommendationService.getPersonalizedRecommendations(
                RecommendationContext.MORNING,
                2);

        assertThat(response.items())
                .extracting(RecommendationItemResponse::id)
                .containsExactly(1L, 2L);
    }

    @Test
    void popularRecommendationsFallbackToLatestAvailableItems() {
        MenuItem latestItem = menuItem(3L, "Soda chanh", "Soda");

        when(orderItemRepository.findPopularAvailableMenuItems(any(Pageable.class)))
                .thenReturn(List.of());
        when(menuItemRepository.findByActiveTrueAndAvailableTrueOrderByIdDesc(any(Pageable.class)))
                .thenReturn(List.of(latestItem));

        RecommendationResponse response = recommendationService.getPopularItems(5);

        assertThat(response.items())
                .extracting(RecommendationItemResponse::id)
                .containsExactly(3L);
    }

    private MenuItem menuItem(Long id, String name, String categoryName) {
        return MenuItem.builder()
                .id(id)
                .name(name)
                .price(BigDecimal.valueOf(30_000))
                .active(true)
                .available(true)
                .category(Category.builder()
                        .id(id)
                        .name(categoryName)
                        .active(true)
                        .build())
                .build();
    }
}
