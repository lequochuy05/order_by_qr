package com.qros.modules.inventory.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.qros.modules.inventory.dto.response.RecipeItemResponse;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.menu.model.MenuItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecipeMapperTest {

    private final RecipeMapper recipeMapper = new RecipeMapper();

    @Test
    void toResponseIncludesStockStatusForKitchenViewer() {
        MenuItem dish = MenuItem.builder().id(10L).name("Phở Bò").build();
        InventoryItem beef = InventoryItem.builder()
                .id(3L)
                .name("Thịt bò")
                .unit("kg")
                .quantityOnHand(new BigDecimal("1.500"))
                .reservedQuantity(new BigDecimal("0.300"))
                .lowStockThreshold(new BigDecimal("2.000"))
                .build();
        RecipeItem recipeItem = RecipeItem.builder()
                .id(99L)
                .menuItem(dish)
                .inventoryItem(beef)
                .quantityRequired(new BigDecimal("0.200"))
                .build();

        RecipeItemResponse response = recipeMapper.toResponse(recipeItem);

        assertThat(response.availableQuantity()).isEqualByComparingTo("1.200");
        assertThat(response.lowStock()).isTrue();
        assertThat(response.outOfStock()).isFalse();
    }

    @Test
    void toResponseMarksOutOfStockWhenAvailableIsZero() {
        MenuItem dish = MenuItem.builder().id(11L).name("Trà Đào").build();
        InventoryItem syrup = InventoryItem.builder()
                .id(4L)
                .name("Syrup Đào")
                .unit("ml")
                .quantityOnHand(new BigDecimal("100.000"))
                .reservedQuantity(new BigDecimal("120.000"))
                .lowStockThreshold(new BigDecimal("50.000"))
                .build();
        RecipeItem recipeItem = RecipeItem.builder()
                .id(100L)
                .menuItem(dish)
                .inventoryItem(syrup)
                .quantityRequired(new BigDecimal("30.000"))
                .build();

        RecipeItemResponse response = recipeMapper.toResponse(recipeItem);

        assertThat(response.availableQuantity()).isEqualByComparingTo("0.000");
        assertThat(response.lowStock()).isTrue();
        assertThat(response.outOfStock()).isTrue();
    }
}
