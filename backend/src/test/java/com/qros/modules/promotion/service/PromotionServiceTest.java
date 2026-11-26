package com.qros.modules.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.promotion.dto.request.PromotionRequest;
import com.qros.modules.promotion.dto.response.PromotionResponse;
import com.qros.modules.promotion.mapper.PromotionMapper;
import com.qros.modules.promotion.model.Promotion;
import com.qros.modules.promotion.repository.PromotionRepository;
import com.qros.shared.event.DomainEvents.PromotionChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(promotionRepository, new PromotionMapper(), eventPublisher);
    }

    @Test
    void searchForManagementNormalizesKeywordAndStatus() {
        Promotion promotion = Promotion.builder()
                .id(1L)
                .name("Lunch Happy Hour")
                .discountPercent(new BigDecimal("15.00"))
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(14, 0))
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .active(true)
                .build();
        PageRequest pageable = PageRequest.of(0, 24);
        when(promotionRepository.searchForManagement(eq("Lunch"), eq("RUNNING"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(promotion)));

        Page<PromotionResponse> result = promotionService.searchForManagement("  Lunch ", " running ", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Lunch Happy Hour");
    }

    @Test
    void searchForManagementTreatsAllStatusAsNull() {
        PageRequest pageable = PageRequest.of(0, 24);
        when(promotionRepository.searchForManagement(isNull(), isNull(), any(), any(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<PromotionResponse> result = promotionService.searchForManagement("   ", "ALL", pageable);

        assertThat(result.getContent()).isEmpty();
        verify(promotionRepository).searchForManagement(isNull(), isNull(), any(), any(), eq(pageable));
    }

    @Test
    void createSavesPromotionAndPublishesChangeEvent() {
        PromotionRequest request = new PromotionRequest(
                "  Weekend Promo  ",
                new BigDecimal("20.00"),
                LocalTime.of(17, 0),
                LocalTime.of(21, 0),
                Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                true);

        when(promotionRepository.existsByNameIgnoreCase("Weekend Promo")).thenReturn(false);
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> {
            Promotion saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        PromotionResponse response = promotionService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Weekend Promo");
        assertThat(response.discountPercent()).isEqualByComparingTo("20.00");
        verify(eventPublisher).publishEvent(any(PromotionChangeEvent.class));
    }

    @Test
    void createThrowsWhenNameAlreadyExists() {
        PromotionRequest request = new PromotionRequest(
                "Happy Hour", new BigDecimal("10.00"), LocalTime.of(14, 0), LocalTime.of(16, 0), Set.of(), true);
        when(promotionRepository.existsByNameIgnoreCase("Happy Hour")).thenReturn(true);

        assertThatThrownBy(() -> promotionService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROMOTION_NAME_EXISTS);
    }

    @Test
    void updateModifiesExistingPromotionAndPublishesEvent() {
        Promotion existing = Promotion.builder()
                .id(5L)
                .name("Old Name")
                .discountPercent(new BigDecimal("10.00"))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .daysOfWeek(new LinkedHashSet<>(Set.of(DayOfWeek.MONDAY)))
                .active(true)
                .build();
        PromotionRequest request = new PromotionRequest(
                "Updated Name",
                new BigDecimal("25.00"),
                LocalTime.of(11, 0),
                LocalTime.of(15, 0),
                Set.of(DayOfWeek.FRIDAY),
                false);

        when(promotionRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(promotionRepository.existsByNameIgnoreCaseAndIdNot("Updated Name", 5L))
                .thenReturn(false);
        when(promotionRepository.save(existing)).thenReturn(existing);

        PromotionResponse response = promotionService.update(5L, request);

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.discountPercent()).isEqualByComparingTo("25.00");
        assertThat(response.active()).isFalse();
        assertThat(response.daysOfWeek()).containsExactly(DayOfWeek.FRIDAY);
        verify(eventPublisher).publishEvent(any(PromotionChangeEvent.class));
    }

    @Test
    void deleteRemovesPromotionAndPublishesEvent() {
        Promotion existing = Promotion.builder().id(8L).name("To Delete").build();
        when(promotionRepository.findById(8L)).thenReturn(Optional.of(existing));

        promotionService.delete(8L);

        verify(promotionRepository).delete(existing);
        verify(eventPublisher).publishEvent(any(PromotionChangeEvent.class));
    }

    @Test
    void findBestActivePromotionSelectsHighestDiscountPercent() {
        Promotion tenPercent = Promotion.builder()
                .id(1L)
                .name("Afternoon 10%")
                .discountPercent(new BigDecimal("10.00"))
                .active(true)
                .build();
        Promotion twentyPercent = Promotion.builder()
                .id(2L)
                .name("Lunch 20%")
                .discountPercent(new BigDecimal("20.00"))
                .active(true)
                .build();

        when(promotionRepository.findAllActive(any(), any())).thenReturn(List.of(tenPercent, twentyPercent));

        Optional<Promotion> best = promotionService.findBestActivePromotion();

        assertThat(best).contains(twentyPercent);
        assertThat(promotionService.calculatePromotionDiscount(new BigDecimal("150000")))
                .isEqualByComparingTo("30000.00");
    }

    @Test
    void calculatePromotionDiscountReturnsZeroWhenNoPromotionOrZeroSubtotal() {
        when(promotionRepository.findAllActive(any(), any())).thenReturn(List.of());

        assertThat(promotionService.calculatePromotionDiscount(new BigDecimal("100000")))
                .isEqualByComparingTo("0.00");
        assertThat(promotionService.calculatePromotionDiscount(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
        assertThat(promotionService.calculatePromotionDiscount(null)).isEqualByComparingTo("0.00");
    }
}
