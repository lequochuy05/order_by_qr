package com.qros.modules.inventory.mapper;

import com.qros.modules.inventory.dto.request.RecipeItemRequest;
import com.qros.modules.inventory.dto.response.RecipeItemResponse;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.menu.model.MenuItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecipeMapper {

    public RecipeItem toEntity(MenuItem menuItem, InventoryItem inventoryItem, RecipeItemRequest request) {
        return RecipeItem.builder()
                .menuItem(menuItem)
                .inventoryItem(inventoryItem)
                .quantityRequired(request.quantityRequired())
                .build();
    }

    public RecipeItemResponse toResponse(RecipeItem recipeItem) {
        MenuItem menuItem = recipeItem.getMenuItem();
        InventoryItem inventoryItem = recipeItem.getInventoryItem();

        return new RecipeItemResponse(
                recipeItem.getId(),
                menuItem != null ? menuItem.getId() : null,
                menuItem != null ? menuItem.getName() : null,
                inventoryItem != null ? inventoryItem.getId() : null,
                inventoryItem != null ? inventoryItem.getName() : null,
                inventoryItem != null ? inventoryItem.getUnit() : null,
                recipeItem.getQuantityRequired());
    }

    public List<RecipeItemResponse> toResponses(List<RecipeItem> recipeItems) {
        return recipeItems.stream().map(this::toResponse).toList();
    }
}
