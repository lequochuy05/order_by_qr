package com.qros.modules.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InventoryAvailabilityServiceTest {

    @Mock
    private RecipeItemRepository recipeItemRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private ComboRepository comboRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private InventoryAvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new InventoryAvailabilityService(
                recipeItemRepository, menuItemRepository, comboRepository, eventPublisher);
    }

    @Test
    void markingMenuItemOutOfStockCascadesToCombosAndViceVersa() {
        MenuItem dish1 = MenuItem.builder()
                .id(10L)
                .name("Pho Bo")
                .active(true)
                .available(false)
                .build();
        MenuItem dish2 = MenuItem.builder()
                .id(20L)
                .name("Tra Da")
                .active(true)
                .available(true)
                .build();

        Combo combo = Combo.builder()
                .id(100L)
                .name("Combo Sang")
                .active(true)
                .available(true)
                .items(new LinkedHashSet<>())
                .build();

        combo.getItems()
                .add(ComboItem.builder()
                        .id(1L)
                        .combo(combo)
                        .menuItem(dish1)
                        .quantity(1)
                        .build());
        combo.getItems()
                .add(ComboItem.builder()
                        .id(2L)
                        .combo(combo)
                        .menuItem(dish2)
                        .quantity(1)
                        .build());

        when(comboRepository.findAllByMenuItemIdsWithItems(List.of(10L))).thenReturn(List.of(combo));

        // 1. Dish 1 marked out of stock -> Combo becomes unavailable
        availabilityService.syncCombosForMenuItem(dish1);
        assertThat(combo.getAvailable()).isFalse();
        verify(comboRepository).save(combo);

        // 2. Dish 1 re-enabled -> Combo automatically becomes available again
        dish1.setAvailable(true);
        when(menuItemRepository.existsById(10L)).thenReturn(true);
        when(recipeItemRepository.findByMenuItemId(10L)).thenReturn(List.of());
        when(recipeItemRepository.findByMenuItemIds(List.of(10L, 20L))).thenReturn(List.of());

        availabilityService.syncCombosForMenuItem(dish1);
        assertThat(combo.getAvailable()).isTrue();
    }

    @Test
    void getComboAvailabilityReturnsFalseWhenAnyConstituentDishIsUnavailable() {
        MenuItem dish1 = MenuItem.builder()
                .id(10L)
                .name("Pho Bo")
                .active(true)
                .available(false)
                .build();
        MenuItem dish2 = MenuItem.builder()
                .id(20L)
                .name("Tra Da")
                .active(true)
                .available(true)
                .build();

        Combo combo = Combo.builder()
                .id(100L)
                .name("Combo Sang")
                .active(true)
                .available(true)
                .items(new LinkedHashSet<>(List.of(
                        ComboItem.builder().id(1L).menuItem(dish1).quantity(1).build(),
                        ComboItem.builder().id(2L).menuItem(dish2).quantity(1).build())))
                .build();

        when(comboRepository.findAllByIdInWithItems(List.of(100L))).thenReturn(List.of(combo));

        Map<Long, Boolean> result = availabilityService.getComboAvailabilityByIds(List.of(100L));

        assertThat(result).containsEntry(100L, false);
    }

    @Test
    void ingredientOutOfStockMarksMenuItemAndComboUnavailableAndRestockReEnablesThem() {
        InventoryItem beef = InventoryItem.builder()
                .id(500L)
                .name("Beef")
                .unit("kg")
                .active(true)
                .quantityOnHand(new BigDecimal("0.100"))
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        MenuItem phoBo = MenuItem.builder()
                .id(10L)
                .name("Pho Bo")
                .active(true)
                .available(true)
                .build();

        MenuItem traDa = MenuItem.builder()
                .id(20L)
                .name("Tra Da")
                .active(true)
                .available(true)
                .build();

        RecipeItem phoBeefRecipe = RecipeItem.builder()
                .id(1000L)
                .menuItem(phoBo)
                .inventoryItem(beef)
                .quantityRequired(new BigDecimal("0.200"))
                .build();

        Combo combo = Combo.builder()
                .id(100L)
                .name("Combo Pho + Tra")
                .active(true)
                .available(true)
                .items(new LinkedHashSet<>(List.of(
                        ComboItem.builder().id(1L).menuItem(phoBo).quantity(1).build(),
                        ComboItem.builder().id(2L).menuItem(traDa).quantity(1).build())))
                .build();

        when(recipeItemRepository.findByInventoryItemIds(List.of(500L))).thenReturn(List.of(phoBeefRecipe));
        when(menuItemRepository.findAllByIdIn(List.of(10L))).thenReturn(List.of(phoBo));
        when(recipeItemRepository.findByMenuItemIds(List.of(10L))).thenReturn(List.of(phoBeefRecipe));
        when(comboRepository.findAllByMenuItemIdsWithItems(List.of(10L))).thenReturn(List.of(combo));
        when(recipeItemRepository.findByMenuItemIds(List.of(10L, 20L))).thenReturn(List.of(phoBeefRecipe));

        // 1. Beef has 0.100 kg (< 0.200 kg required) -> both Pho Bo and Combo become unavailable
        availabilityService.syncAvailabilityForInventoryItems(List.of(500L), false);

        assertThat(phoBo.getAvailable()).isFalse();
        assertThat(combo.getAvailable()).isFalse();

        // 2. Beef is restocked to 2.000 kg -> both Pho Bo and Combo become available again
        beef.setQuantityOnHand(new BigDecimal("2.000"));
        availabilityService.syncAvailabilityForInventoryItems(List.of(500L), true);

        assertThat(phoBo.getAvailable()).isTrue();
        assertThat(combo.getAvailable()).isTrue();
    }
}
