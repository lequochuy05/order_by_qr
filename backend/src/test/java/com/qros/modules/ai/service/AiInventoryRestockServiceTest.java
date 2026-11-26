package com.qros.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.dto.request.AiRestockSuggestionRequest;
import com.qros.modules.ai.dto.response.AiRestockSuggestionResponse;
import com.qros.modules.ai.dto.response.AiRestockSuggestionResponse.RestockItemProposal;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.dto.projection.MenuItemDemandProjection;
import com.qros.modules.analytics.service.AnalyticsLookupService;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.service.InventoryLookupService;
import com.qros.modules.menu.model.MenuItem;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiInventoryRestockServiceTest {

    private InventoryLookupService inventoryLookupService;
    private AnalyticsLookupService analyticsLookupService;
    private AiChatGateway aiChatGateway;
    private AiInventoryRestockService service;

    @BeforeEach
    void setUp() {
        inventoryLookupService = mock(InventoryLookupService.class);
        analyticsLookupService = mock(AnalyticsLookupService.class);
        aiChatGateway = mock(AiChatGateway.class);

        service = new AiInventoryRestockService(
                inventoryLookupService,
                analyticsLookupService,
                aiChatGateway,
                new AiPromptBuilder(),
                new ObjectMapper(),
                new SimpleMeterRegistry());
    }

    @Test
    void projectsBomDemandAndMergesValidAiReasons() {
        InventoryItem beef = InventoryItem.builder()
                .id(1L)
                .name("Thịt bò Mỹ")
                .unit("kg")
                .quantityOnHand(new BigDecimal("0.500"))
                .reservedQuantity(BigDecimal.ZERO)
                .lowStockThreshold(new BigDecimal("2.000"))
                .unitCost(new BigDecimal("220000.00"))
                .active(true)
                .build();
        InventoryItem herb = InventoryItem.builder()
                .id(2L)
                .name("Hành lá")
                .unit("kg")
                .quantityOnHand(new BigDecimal("5.000"))
                .reservedQuantity(BigDecimal.ZERO)
                .lowStockThreshold(new BigDecimal("1.000"))
                .unitCost(new BigDecimal("30000.00"))
                .active(true)
                .build();

        MenuItem pho = MenuItem.builder().id(10L).name("Phở Bò").build();
        RecipeItem beefRecipe = RecipeItem.builder()
                .id(100L)
                .menuItem(pho)
                .inventoryItem(beef)
                .quantityRequired(new BigDecimal("0.250"))
                .build();

        when(inventoryLookupService.findActiveInventoryItemsOrdered()).thenReturn(List.of(beef, herb));
        when(analyticsLookupService.menuItemDemandIncludingCombos(any(), any()))
                .thenReturn(List.of(demandItem(10L, 60L)));
        when(inventoryLookupService.findAllActiveRecipesWithMenuAndInventory()).thenReturn(List.of(beefRecipe));
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("""
                {
                  "summary": "Cần nhập gấp Thịt bò Mỹ cho 7 ngày tới.",
                  "recommendations": ["Đặt ngay 4 kg Thịt bò Mỹ trước cuối ngày."],
                  "itemReasons": [
                    {"inventoryItemId": 1, "reason": "Bán chạy Phở Bò, chỉ còn đủ 1 ngày."}
                  ]
                }
                """);

        AiRestockSuggestionResponse response = service.suggestRestock(new AiRestockSuggestionRequest(7, "vi"));

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.forecastDays()).isEqualTo(7);
        assertThat(response.criticalCount()).isEqualTo(1);
        assertThat(response.highCount()).isEqualTo(0);
        assertThat(response.items()).hasSize(2);

        RestockItemProposal criticalItem = response.items().get(0);
        assertThat(criticalItem.inventoryItemId()).isEqualTo(1L);
        assertThat(criticalItem.urgency()).isEqualTo("CRITICAL");
        // 60 / 30 * 0.25 = 0.500 kg/day -> 0.500 available => 1.0 days left
        assertThat(criticalItem.daysUntilStockout()).isEqualTo(1.0);
        // 0.5 * 7 * 1.10 = 3.850 projected - 0.500 available = 3.350 shortfall
        assertThat(criticalItem.projectedUsage()).isEqualByComparingTo("3.850");
        assertThat(criticalItem.shortfall()).isEqualByComparingTo("3.350");
        assertThat(criticalItem.suggestedRestockQty()).isEqualByComparingTo("3.350");
        assertThat(criticalItem.estimatedCost()).isEqualByComparingTo("737000.00");
        assertThat(criticalItem.drivenByDishes()).containsExactly("Phở Bò");
        assertThat(criticalItem.reason()).contains("Phở Bò");

        RestockItemProposal okItem = response.items().get(1);
        assertThat(okItem.urgency()).isEqualTo("OK");
        assertThat(okItem.daysUntilStockout()).isNull();
    }

    @Test
    void usesEnglishDeterministicFallbackWhenAiGatewayUnavailable() {
        InventoryItem milk = InventoryItem.builder()
                .id(3L)
                .name("Sữa tươi")
                .unit("l")
                .quantityOnHand(new BigDecimal("1.500"))
                .reservedQuantity(BigDecimal.ZERO)
                .lowStockThreshold(new BigDecimal("3.000"))
                .unitCost(new BigDecimal("35000.00"))
                .active(true)
                .build();

        when(inventoryLookupService.findActiveInventoryItemsOrdered()).thenReturn(List.of(milk));
        when(analyticsLookupService.menuItemDemandIncludingCombos(any(), any())).thenReturn(List.of());
        when(inventoryLookupService.findAllActiveRecipesWithMenuAndInventory()).thenReturn(List.of());
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("Tính năng trợ lý AI hiện chưa được bật.");

        AiRestockSuggestionResponse response = service.suggestRestock(new AiRestockSuggestionRequest(14, "en"));

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.language()).isEqualTo("en");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).urgency()).isEqualTo("MEDIUM");
        assertThat(response.items().get(0).suggestedRestockQty()).isEqualByComparingTo("1.500");
        assertThat(response.summary()).contains("14 days");
    }

    private static MenuItemDemandProjection demandItem(Long id, long qty) {
        return new MenuItemDemandProjection() {
            @Override
            public Long getMenuItemId() {
                return id;
            }

            @Override
            public Long getQuantitySold() {
                return qty;
            }
        };
    }
}
