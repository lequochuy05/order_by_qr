package com.qros.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.dto.request.AiRecipeAssistantRequest;
import com.qros.modules.ai.dto.request.AiRecipeAssistantRequest.DraftRecipeLine;
import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse;
import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse.FoodCostAnalysis;
import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse.SuggestedRecipeLine;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiRecipeAssistantServiceTest {

    private MenuItemRepository menuItemRepository;
    private RecipeItemRepository recipeItemRepository;
    private InventoryItemRepository inventoryItemRepository;
    private AiChatGateway aiChatGateway;
    private AiRecipeAssistantService service;

    @BeforeEach
    void setUp() {
        menuItemRepository = mock(MenuItemRepository.class);
        recipeItemRepository = mock(RecipeItemRepository.class);
        inventoryItemRepository = mock(InventoryItemRepository.class);
        aiChatGateway = mock(AiChatGateway.class);

        service = new AiRecipeAssistantService(
                menuItemRepository,
                recipeItemRepository,
                inventoryItemRepository,
                aiChatGateway,
                new AiPromptBuilder(),
                new ObjectMapper(),
                new SimpleMeterRegistry());
    }

    @Test
    void throwsNotFoundWhenMenuItemMissing() {
        when(menuItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assistRecipe(new AiRecipeAssistantRequest(999L, null, "vi")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MENU_ITEM_NOT_FOUND);
    }

    @Test
    void computesFoodCostStatusThresholdsDeterministically() {
        FoodCostAnalysis noRecipe =
                AiRecipeAssistantService.computeFoodCostAnalysis(new BigDecimal("50000"), List.of());
        assertThat(noRecipe.marginStatus()).isEqualTo("NO_RECIPE");
        assertThat(noRecipe.foodCostPercent()).isNull();

        FoodCostAnalysis missingCost = AiRecipeAssistantService.computeFoodCostAnalysis(
                new BigDecimal("50000"),
                List.of(new SuggestedRecipeLine(
                        1L, "Thịt bò", "kg", new BigDecimal("0.200"), BigDecimal.ZERO, BigDecimal.ZERO, "")));
        assertThat(missingCost.marginStatus()).isEqualTo("MISSING_COST");
        assertThat(missingCost.missingUnitCostCount()).isEqualTo(1);

        FoodCostAnalysis healthy = AiRecipeAssistantService.computeFoodCostAnalysis(
                new BigDecimal("50000"),
                List.of(new SuggestedRecipeLine(
                        1L,
                        "Gạo",
                        "kg",
                        new BigDecimal("0.300"),
                        new BigDecimal("50000"),
                        new BigDecimal("15000"),
                        "")));
        assertThat(healthy.marginStatus()).isEqualTo("HEALTHY");
        assertThat(healthy.foodCostPercent()).isEqualTo(30.0);
        assertThat(healthy.grossProfit()).isEqualByComparingTo("35000.00");

        FoodCostAnalysis warning = AiRecipeAssistantService.computeFoodCostAnalysis(
                new BigDecimal("50000"),
                List.of(new SuggestedRecipeLine(
                        1L,
                        "Thịt gà",
                        "kg",
                        new BigDecimal("0.200"),
                        new BigDecimal("100000"),
                        new BigDecimal("20000"),
                        "")));
        assertThat(warning.marginStatus()).isEqualTo("WARNING");
        assertThat(warning.foodCostPercent()).isEqualTo(40.0);

        // 25000 / 50000 = 50% (>45% CRITICAL); minPrice = ceil1000(25000 / 0.35 = 71428.57) = 72000
        FoodCostAnalysis critical = AiRecipeAssistantService.computeFoodCostAnalysis(
                new BigDecimal("50000"),
                List.of(new SuggestedRecipeLine(
                        1L,
                        "Thịt bò",
                        "kg",
                        new BigDecimal("0.100"),
                        new BigDecimal("250000"),
                        new BigDecimal("25000"),
                        "")));
        assertThat(critical.marginStatus()).isEqualTo("CRITICAL");
        assertThat(critical.foodCostPercent()).isEqualTo(50.0);
        assertThat(critical.recommendedMinPrice()).isEqualByComparingTo("72000.00");
    }

    @Test
    void stripsHallucinatedInventoryIdsAndHydratesLiveCosts() {
        MenuItem steak = MenuItem.builder()
                .id(15L)
                .name("Bít Tết Bò")
                .price(new BigDecimal("100000.00"))
                .category(Category.builder().id(1L).name("Món chính").build())
                .build();
        InventoryItem beef = InventoryItem.builder()
                .id(1L)
                .name("Thăn bò Úc")
                .unit("kg")
                .unitCost(new BigDecimal("300000.00"))
                .active(true)
                .build();

        when(menuItemRepository.findById(15L)).thenReturn(Optional.of(steak));
        when(inventoryItemRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(beef));
        when(recipeItemRepository.findByMenuItemId(15L)).thenReturn(List.of());
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("""
                {
                  "suggestedItems": [
                    {"inventoryItemId": 1, "quantityRequired": 0.180, "reason": "Phần thăn bò chính"},
                    {"inventoryItemId": 999, "quantityRequired": 0.050, "reason": "Hallucinated truffle oil"}
                  ],
                  "costWarnings": ["Giá vốn ở mức 54% cần cân nhắc chỉnh giá hoặc giảm định lượng."],
                  "pricingAdvice": ["Nâng giá lên tối thiểu 155.000đ."]
                }
                """);

        AiRecipeAssistantResponse response = service.assistRecipe(
                new AiRecipeAssistantRequest(15L, List.of(new DraftRecipeLine(1L, new BigDecimal("0.200"))), "vi"));

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.currentAnalysis().totalFoodCost()).isEqualByComparingTo("60000.00");
        assertThat(response.currentAnalysis().marginStatus()).isEqualTo("CRITICAL");
        assertThat(response.suggestedItems()).hasSize(1);
        assertThat(response.suggestedItems().get(0).inventoryItemId()).isEqualTo(1L);
        assertThat(response.suggestedItems().get(0).lineCost()).isEqualByComparingTo("54000.00");
        assertThat(response.suggestedAnalysis().totalFoodCost()).isEqualByComparingTo("54000.00");
        assertThat(response.suggestedAnalysis().foodCostPercent()).isEqualTo(54.0);
    }

    @Test
    void usesBilingualFallbackWhenAiUnavailableAndMatchesKeywordOrSavedBom() {
        MenuItem coffee = MenuItem.builder()
                .id(20L)
                .name("Cà phê sữa đá")
                .price(new BigDecimal("35000.00"))
                .build();
        InventoryItem bean = InventoryItem.builder()
                .id(5L)
                .name("Hạt cà phê")
                .unit("kg")
                .unitCost(new BigDecimal("200000.00"))
                .active(true)
                .build();

        when(menuItemRepository.findById(20L)).thenReturn(Optional.of(coffee));
        when(inventoryItemRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(bean));
        when(recipeItemRepository.findByMenuItemId(20L))
                .thenReturn(List.of(RecipeItem.builder()
                        .id(55L)
                        .menuItem(coffee)
                        .inventoryItem(bean)
                        .quantityRequired(new BigDecimal("0.040"))
                        .build()));
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("Tính năng trợ lý AI hiện chưa được bật.");

        AiRecipeAssistantResponse response = service.assistRecipe(new AiRecipeAssistantRequest(20L, null, "en"));

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.language()).isEqualTo("en");
        assertThat(response.currentAnalysis().totalFoodCost()).isEqualByComparingTo("8000.00");
        assertThat(response.currentAnalysis().marginStatus()).isEqualTo("HEALTHY");
        assertThat(response.suggestedItems()).hasSize(1);
        assertThat(response.pricingAdvice()).isNotEmpty();
    }
}
