package com.qros.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.dto.request.AiMerchandisingRequest;
import com.qros.modules.ai.dto.response.AiComboSuggestionsResponse;
import com.qros.modules.ai.dto.response.AiComboSuggestionsResponse.ComboProposal;
import com.qros.modules.ai.dto.response.AiPromotionSuggestionsResponse;
import com.qros.modules.ai.dto.response.AiPromotionSuggestionsResponse.PromotionProposal;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.service.MenuContextProvider.MenuCatalogSnapshot;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.dto.projection.DayOfWeekTrafficProjection;
import com.qros.modules.analytics.dto.projection.HourlyTrafficProjection;
import com.qros.modules.analytics.dto.projection.TopSellingItemProjection;
import com.qros.modules.analytics.service.AnalyticsLookupService;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.modules.order.service.OrderLookupService;
import com.qros.modules.promotion.service.PromotionLookupService;
import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.service.SystemSettingsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiMerchandisingServiceTest {

    private MenuContextProvider menuContextProvider;
    private MenuCatalogLookupService menuCatalogLookupService;
    private OrderLookupService orderLookupService;
    private AnalyticsLookupService analyticsLookupService;
    private PromotionLookupService promotionLookupService;
    private SystemSettingsService systemSettingsService;
    private AiChatGateway aiChatGateway;
    private AiMerchandisingService service;

    @BeforeEach
    void setUp() {
        menuContextProvider = mock(MenuContextProvider.class);
        menuCatalogLookupService = mock(MenuCatalogLookupService.class);
        orderLookupService = mock(OrderLookupService.class);
        analyticsLookupService = mock(AnalyticsLookupService.class);
        promotionLookupService = mock(PromotionLookupService.class);
        systemSettingsService = mock(SystemSettingsService.class);
        aiChatGateway = mock(AiChatGateway.class);

        service = new AiMerchandisingService(
                menuContextProvider,
                menuCatalogLookupService,
                orderLookupService,
                analyticsLookupService,
                promotionLookupService,
                systemSettingsService,
                aiChatGateway,
                new AiPromptBuilder(),
                new ObjectMapper(),
                new SimpleMeterRegistry());
    }

    @Test
    void suggestCombosFiltersInvalidIdsClampsPriceAndDeduplicatesExistingCombos() {
        Category main = Category.builder().id(1L).name("Món chính").active(true).build();
        Category drink = Category.builder().id(2L).name("Đồ uống").active(true).build();
        MenuItem item1 = MenuItem.builder()
                .id(1L)
                .name("Bún Bò")
                .price(new BigDecimal("55000.00"))
                .category(main)
                .build();
        MenuItem item2 = MenuItem.builder()
                .id(2L)
                .name("Trà Đào")
                .price(new BigDecimal("25000.00"))
                .category(drink)
                .build();
        MenuItem item3 = MenuItem.builder()
                .id(3L)
                .name("Cà Phê Sữa")
                .price(new BigDecimal("29000.00"))
                .category(drink)
                .build();

        Map<Long, MenuItem> catalog = new LinkedHashMap<>();
        catalog.put(1L, item1);
        catalog.put(2L, item2);
        catalog.put(3L, item3);
        when(menuContextProvider.loadAvailableCatalog()).thenReturn(new MenuCatalogSnapshot(catalog, Map.of()));

        // Existing combo already bundles item1 + item3
        Combo existing = Combo.builder()
                .id(50L)
                .name("Combo Sáng")
                .price(new BigDecimal("72000"))
                .build();
        existing.setItems(Set.of(
                ComboItem.builder()
                        .id(501L)
                        .combo(existing)
                        .menuItem(item1)
                        .quantity(1)
                        .build(),
                ComboItem.builder()
                        .id(502L)
                        .combo(existing)
                        .menuItem(item3)
                        .quantity(1)
                        .build()));
        when(menuCatalogLookupService.findAllActiveCombosWithItems()).thenReturn(List.of(existing));
        when(orderLookupService.findFrequentlyPairedMenuItems(any(), any())).thenReturn(List.of());
        when(analyticsLookupService.topSellingItems(any(), any(), anyInt())).thenReturn(List.of());

        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("""
                {
                  "summary": "Gợi ý ghép món trưa đắt khách.",
                  "combos": [
                    {
                      "name": "Combo Duplicate",
                      "items": [{"menuItemId": 1, "quantity": 1}, {"menuItemId": 3, "quantity": 1}],
                      "suggestedPrice": 75000,
                      "rationale": "Should be deduplicated"
                    },
                    {
                      "name": "Combo Trưa Đậm Đà",
                      "items": [{"menuItemId": 1, "quantity": 1}, {"menuItemId": 999, "quantity": 1}, {"menuItemId": 2, "quantity": 1}],
                      "suggestedPrice": 99000,
                      "rationale": "Cặp món bán chạy"
                    },
                    {
                      "name": "Invalid One Item",
                      "items": [{"menuItemId": 2, "quantity": 1}],
                      "suggestedPrice": 20000
                    }
                  ]
                }
                """);

        AiComboSuggestionsResponse response = service.suggestCombos(new AiMerchandisingRequest("vi"));

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.suggestions()).hasSize(1);
        ComboProposal proposal = response.suggestions().get(0);
        assertThat(proposal.name()).isEqualTo("Combo Trưa Đậm Đà");
        assertThat(proposal.items()).extracting(i -> i.menuItemId()).containsExactly(1L, 2L);
        assertThat(proposal.originalTotalPrice()).isEqualByComparingTo("80000.00");
        assertThat(proposal.suggestedPrice()).isLessThan(proposal.originalTotalPrice());
        assertThat(proposal.discountPercent()).isBetween(1, 50);
    }

    @Test
    void suggestCombosFallsBackToCoOccurrencePairsWhenAiUnavailable() {
        Category main = Category.builder().id(1L).name("Món chính").active(true).build();
        MenuItem m1 = MenuItem.builder()
                .id(11L)
                .name("Cơm Tấm")
                .price(new BigDecimal("45000.00"))
                .category(main)
                .build();
        MenuItem m2 = MenuItem.builder()
                .id(12L)
                .name("Trà Chanh")
                .price(new BigDecimal("15000.00"))
                .category(main)
                .build();
        when(menuContextProvider.loadAvailableCatalog())
                .thenReturn(new MenuCatalogSnapshot(Map.of(11L, m1, 12L, m2), Map.of()));
        when(menuCatalogLookupService.findAllActiveCombosWithItems()).thenReturn(List.of());
        when(analyticsLookupService.topSellingItems(any(), any(), anyInt()))
                .thenReturn(List.of(topSellerWithNullId("Combo Gia Đình", 9L)));
        when(orderLookupService.findFrequentlyPairedMenuItems(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {11L, "Cơm Tấm", 12L, "Trà Chanh", 18L}));
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("Xin lỗi, hệ thống bận.");

        AiComboSuggestionsResponse response = service.suggestCombos(new AiMerchandisingRequest("en"));

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.suggestions()).hasSize(1);
        ComboProposal combo = response.suggestions().get(0);
        assertThat(combo.originalTotalPrice()).isEqualByComparingTo("60000.00");
        assertThat(combo.suggestedPrice()).isLessThan(combo.originalTotalPrice());
        assertThat(combo.rationale()).contains("18");
    }

    @Test
    void suggestPromotionsClampsHoursDiscountAndWeekdays() {
        when(systemSettingsService.getPublicSettings()).thenReturn(settings(LocalTime.of(8, 0), LocalTime.of(22, 0)));
        when(analyticsLookupService.hourlyOrderTraffic(any(), any())).thenReturn(List.of());
        when(analyticsLookupService.dayOfWeekOrderTraffic(any(), any())).thenReturn(List.of());
        when(promotionLookupService.findAllWithDays()).thenReturn(List.of());
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("""
                {
                  "summary": "Tối ưu khung giờ thấp điểm.",
                  "promotions": [
                    {
                      "name": "Early Bird",
                      "discountPercent": 60,
                      "startTime": "06:30",
                      "endTime": "10:00",
                      "daysOfWeek": ["MONDAY", "FUNDAY"],
                      "rationale": "Morning quiet window"
                    },
                    {
                      "name": "Broken Inverted Window",
                      "discountPercent": 15,
                      "startTime": "18:00",
                      "endTime": "15:00",
                      "daysOfWeek": ["TUESDAY"]
                    }
                  ]
                }
                """);

        AiPromotionSuggestionsResponse response = service.suggestPromotions(new AiMerchandisingRequest("vi"));

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.suggestions()).hasSize(1);
        PromotionProposal promo = response.suggestions().get(0);
        assertThat(promo.startTime()).isEqualTo("08:00");
        assertThat(promo.endTime()).isEqualTo("10:00");
        assertThat(promo.discountPercent()).isEqualTo(40.0);
        assertThat(promo.daysOfWeek()).containsExactly("MONDAY");
    }

    @Test
    void suggestPromotionsUsesTrafficFallbackWithinOpeningHours() {
        when(systemSettingsService.getPublicSettings()).thenReturn(settings(LocalTime.of(9, 0), LocalTime.of(21, 0)));
        when(analyticsLookupService.hourlyOrderTraffic(any(), any()))
                .thenReturn(List.of(hourly(12, 40L), hourly(19, 55L)));
        when(analyticsLookupService.dayOfWeekOrderTraffic(any(), any()))
                .thenReturn(List.of(dayTraffic(6, 50L), dayTraffic(7, 65L)));
        when(promotionLookupService.findAllWithDays()).thenReturn(List.of());
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("Tính năng trợ lý AI hiện chưa được bật.");

        AiPromotionSuggestionsResponse response = service.suggestPromotions(new AiMerchandisingRequest("vi"));

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.suggestions()).isNotEmpty();
        PromotionProposal first = response.suggestions().get(0);
        assertThat(LocalTime.parse(first.startTime())).isAfterOrEqualTo(LocalTime.of(9, 0));
        assertThat(LocalTime.parse(first.endTime())).isBeforeOrEqualTo(LocalTime.of(21, 0));
    }

    private static PublicSettingsResponse settings(LocalTime open, LocalTime close) {
        return new PublicSettingsResponse(
                "QROS", "", "", "", "", open, close, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    private static TopSellingItemProjection topSellerWithNullId(String name, long qty) {
        return new TopSellingItemProjection() {
            @Override
            public Long getMenuItemId() {
                return null;
            }

            @Override
            public String getItemName() {
                return name;
            }

            @Override
            public String getCategoryName() {
                return "Combo";
            }

            @Override
            public String getImageUrl() {
                return null;
            }

            @Override
            public Long getQuantitySold() {
                return qty;
            }

            @Override
            public BigDecimal getRevenue() {
                return BigDecimal.valueOf(qty * 99000L);
            }
        };
    }

    private static HourlyTrafficProjection hourly(int hour, long count) {
        return new HourlyTrafficProjection() {
            @Override
            public Integer getHourOfDay() {
                return hour;
            }

            @Override
            public Long getOrderCount() {
                return count;
            }

            @Override
            public BigDecimal getRevenue() {
                return BigDecimal.valueOf(count * 100000L);
            }
        };
    }

    private static DayOfWeekTrafficProjection dayTraffic(int isoDay, long count) {
        return new DayOfWeekTrafficProjection() {
            @Override
            public Integer getIsoDayOfWeek() {
                return isoDay;
            }

            @Override
            public Long getOrderCount() {
                return count;
            }

            @Override
            public BigDecimal getRevenue() {
                return BigDecimal.valueOf(count * 100000L);
            }
        };
    }
}
