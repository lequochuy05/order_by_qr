package com.qros.modules.inventory.service;

import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Module-boundary lookup facade for inventory items and BOM recipes consumed by other modules.
 */
@Service
@RequiredArgsConstructor
public class InventoryLookupService {

    private final InventoryItemRepository inventoryItemRepository;
    private final RecipeItemRepository recipeItemRepository;

    public List<InventoryItem> findActiveInventoryItemsOrdered() {
        return inventoryItemRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<InventoryItem> findInventoryItemsByIds(@NonNull Iterable<Long> ids) {
        return inventoryItemRepository.findAllById(ids);
    }

    public long countOutOfStockActiveItems() {
        return inventoryItemRepository.countOutOfStockActiveItems();
    }

    public List<InventoryItem> findLowStockActiveItems() {
        return inventoryItemRepository.findLowStockActiveItems();
    }

    public List<RecipeItem> findAllActiveRecipesWithMenuAndInventory() {
        return recipeItemRepository.findAllActiveWithMenuAndInventory();
    }

    public List<RecipeItem> findRecipesByMenuItemId(Long menuItemId) {
        return recipeItemRepository.findByMenuItemId(menuItemId);
    }
}
