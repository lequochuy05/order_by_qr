package com.qros.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.response.AiChatResponse;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.service.MenuContextProvider.MenuCatalogSnapshot;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.inventory.service.InventoryAvailabilityService;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiAssistantServiceTest {

    private MenuContextProvider menuContextProvider;
    private SystemSettingsContextProvider settingsContextProvider;
    private AiChatGateway aiChatGateway;
    private AiAssistantService service;

    @BeforeEach
    void setUp() {
        menuContextProvider = mock(MenuContextProvider.class);
        settingsContextProvider = mock(SystemSettingsContextProvider.class);
        aiChatGateway = mock(AiChatGateway.class);

        when(menuContextProvider.buildCurrentMenuContext()).thenReturn("MOCK MENU");
        when(settingsContextProvider.buildSettingsContext()).thenReturn("MOCK SETTINGS");

        service = new AiAssistantService(
                menuContextProvider,
                settingsContextProvider,
                aiChatGateway,
                new AiPromptBuilder(),
                new ObjectMapper(),
                new SimpleMeterRegistry());
    }

    @Test
    void hydratesCanonicalCatalogDetailsAndFiltersInvalidOrDuplicateSuggestions() {
        Category noodles =
                Category.builder().id(1L).name("Mì & Bún").active(true).build();
        ItemOption sizeOption = ItemOption.builder().id(11L).name("Size").build();
        MenuItem ramen = MenuItem.builder()
                .id(10L)
                .name("Mì Ramen Tonkotsu")
                .price(new BigDecimal("85000"))
                .img("https://cdn.example/ramen.jpg")
                .category(noodles)
                .active(true)
                .available(true)
                .itemOptions(Set.of(sizeOption))
                .build();
        MenuItem tea = MenuItem.builder()
                .id(20L)
                .name("Trà Đào Cam Sả")
                .price(new BigDecimal("35000"))
                .img("https://cdn.example/tea.jpg")
                .category(noodles)
                .active(true)
                .available(true)
                .itemOptions(Set.of())
                .build();

        Combo combo = Combo.builder()
                .id(5L)
                .name("Combo Cặp Đôi")
                .price(new BigDecimal("110000"))
                .active(true)
                .available(true)
                .items(new LinkedHashSet<>(List.of(
                        ComboItem.builder().id(51L).menuItem(ramen).quantity(1).build())))
                .build();

        when(menuContextProvider.loadAvailableCatalog())
                .thenReturn(new MenuCatalogSnapshot(Map.of(10L, ramen, 20L, tea), Map.of(5L, combo)));

        String modelOutput = """
                ```json
                {
                  "reply": "Bạn thử [ITEM:10] Mì Ramen và Combo Cặp Đôi nhé!",
                  "suggestions": [
                    {"type": "MENU_ITEM", "id": 10, "quantity": 2, "reason": "Nước dùng đậm đà [ITEM:10]"},
                    {"type": "MENU_ITEM", "id": 10, "quantity": 9, "reason": "Trùng món sẽ bị loại"},
                    {"type": "MENU_ITEM", "id": 999, "quantity": 1, "reason": "ID ảo không tồn tại"},
                    {"type": "COMBO", "id": 5, "quantity": 99, "reason": "Tiết kiệm cho 2 người"},
                    {"type": "MENU_ITEM", "id": 20, "quantity": 0, "reason": "Giải nhiệt thanh mát"}
                  ]
                }
                ```
                """;
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn(modelOutput);

        AiChatResponse response = service.chat(new AiChatRequest("Gợi ý cho 2 người", List.of()));

        assertThat(response.reply()).isEqualTo("Bạn thử Mì Ramen và Combo Cặp Đôi nhé!");
        assertThat(response.suggestions()).hasSize(3);

        assertThat(response.suggestions().get(0).id()).isEqualTo(10L);
        assertThat(response.suggestions().get(0).type()).isEqualTo("MENU_ITEM");
        assertThat(response.suggestions().get(0).name()).isEqualTo("Mì Ramen Tonkotsu");
        assertThat(response.suggestions().get(0).price()).isEqualByComparingTo("85000");
        assertThat(response.suggestions().get(0).hasOptions()).isTrue();
        assertThat(response.suggestions().get(0).quantity()).isEqualTo(2);
        assertThat(response.suggestions().get(0).reason()).isEqualTo("Nước dùng đậm đà");

        assertThat(response.suggestions().get(1).id()).isEqualTo(5L);
        assertThat(response.suggestions().get(1).type()).isEqualTo("COMBO");
        assertThat(response.suggestions().get(1).quantity()).isEqualTo(10);
        assertThat(response.suggestions().get(1).imageUrl()).isEqualTo("https://cdn.example/ramen.jpg");
        assertThat(response.suggestions().get(1).hasOptions()).isFalse();

        assertThat(response.suggestions().get(2).id()).isEqualTo(20L);
        assertThat(response.suggestions().get(2).quantity()).isEqualTo(1);
        assertThat(response.suggestions().get(2).hasOptions()).isFalse();
    }

    @Test
    void capsSuggestionsAtFiveEvenWhenModelSuggestsMore() {
        Category cat = Category.builder().id(1L).name("Món chính").active(true).build();
        Map<Long, MenuItem> map = new LinkedHashMap<>();
        StringBuilder itemsJson = new StringBuilder();
        for (long i = 1; i <= 7; i++) {
            map.put(
                    i,
                    MenuItem.builder()
                            .id(i)
                            .name("Món " + i)
                            .price(new BigDecimal("50000"))
                            .category(cat)
                            .active(true)
                            .available(true)
                            .build());
            if (i > 1) itemsJson.append(",");
            itemsJson
                    .append("{\"type\":\"MENU_ITEM\",\"id\":")
                    .append(i)
                    .append(",\"quantity\":1,\"reason\":\"Ngon\"}");
        }

        when(menuContextProvider.loadAvailableCatalog()).thenReturn(new MenuCatalogSnapshot(map, Map.of()));
        when(aiChatGateway.chat(any(), anyString(), anyString()))
                .thenReturn("{\"reply\":\"Gợi ý thực đơn\",\"suggestions\":[" + itemsJson + "]}");

        AiChatResponse response = service.chat(new AiChatRequest("Ăn gì ngon?", null));

        assertThat(response.suggestions()).hasSize(5);
    }

    @Test
    void fallsBackGracefullyToPlainTextWhenModelOutputIsNotValidJson() {
        when(aiChatGateway.chat(any(), anyString(), anyString()))
                .thenReturn("Quán hiện mở cửa từ 08:00 đến 22:00 hàng ngày nhé bạn!");

        AiChatResponse response = service.chat(new AiChatRequest("Quán mấy giờ đóng cửa?", List.of()));

        assertThat(response.reply()).isEqualTo("Quán hiện mở cửa từ 08:00 đến 22:00 hàng ngày nhé bạn!");
        assertThat(response.suggestions()).isEmpty();
        verify(menuContextProvider).buildCurrentMenuContext();
        verifyNoMoreInteractions(menuContextProvider);
    }

    @Test
    void menuContextProviderExcludesOutOfStockItemsAndTagsIdsForCatalogLookup() {
        MenuCatalogLookupService menuLookup = mock(MenuCatalogLookupService.class);
        InventoryAvailabilityService availabilityService = mock(InventoryAvailabilityService.class);

        Category drinks = Category.builder().id(2L).name("Đồ uống").active(true).build();
        MenuItem inStock = MenuItem.builder()
                .id(12L)
                .name("Trà Sữa Trân Châu")
                .price(new BigDecimal("42000"))
                .category(drinks)
                .active(true)
                .available(true)
                .itemOptions(Set.of(ItemOption.builder().id(3L).name("Đá").build()))
                .build();
        MenuItem lowIngredients = MenuItem.builder()
                .id(13L)
                .name("Cà Phê Muối")
                .price(new BigDecimal("39000"))
                .category(drinks)
                .active(true)
                .available(true)
                .build();

        when(menuLookup.findActiveCategories()).thenReturn(List.of(drinks));
        when(menuLookup.findAllPublicAvailableMenuItems()).thenReturn(List.of(inStock, lowIngredients));
        when(availabilityService.getMenuItemAvailability(List.of(12L, 13L))).thenReturn(Map.of(12L, true, 13L, false));
        when(menuLookup.findAllActiveCombosWithItems()).thenReturn(List.of());
        when(availabilityService.getComboAvailabilityByIds(anyList())).thenReturn(Map.of());

        MenuContextProvider provider = new MenuContextProvider(menuLookup, availabilityService);

        String context = provider.buildCurrentMenuContext();
        MenuCatalogSnapshot snapshot = provider.loadAvailableCatalog();

        assertThat(context).contains("[ITEM:12] Trà Sữa Trân Châu").contains("Có tùy chọn");
        assertThat(context).doesNotContain("Cà Phê Muối");
        assertThat(snapshot.menuItemsById()).containsOnlyKeys(12L);
    }
}
