package com.qros.modules.inventory.service;

import com.qros.modules.inventory.dto.request.RecipeItemRequest;
import com.qros.modules.inventory.dto.request.RecipeUpdateRequest;
import com.qros.modules.inventory.dto.response.RecipeItemResponse;
import com.qros.modules.inventory.mapper.RecipeMapper;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.MenuItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecipeService {

        private final RecipeItemRepository recipeItemRepository;
        private final MenuItemRepository menuItemRepository;
        private final InventoryItemRepository inventoryItemRepository;
        private final RecipeMapper recipeMapper;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional(readOnly = true)
        public List<RecipeItemResponse> getRecipeByMenuItemId(@NonNull Long menuItemId) {
                return recipeMapper.toResponses(
                                recipeItemRepository.findByMenuItemId(menuItemId));
        }

        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = CacheNames.INVENTORY, allEntries = true),
                        @CacheEvict(value = CacheNames.PUBLIC_MENU, allEntries = true),
                        @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
                        @CacheEvict(value = CacheNames.AI_MENU_CONTEXT, allEntries = true)
        })
        public List<RecipeItemResponse> updateRecipe(
                        @NonNull Long menuItemId,
                        @NonNull RecipeUpdateRequest request) {
                MenuItem menuItem = getMenuItem(menuItemId);

                validateDuplicateInventoryItems(request.items());

                List<RecipeItem> currentRecipeItems = recipeItemRepository.findByMenuItemId(menuItemId);

                if (!currentRecipeItems.isEmpty()) {
                        recipeItemRepository.deleteAll(currentRecipeItems);
                        recipeItemRepository.flush();
                }

                List<RecipeItem> newRecipeItems = request.items()
                                .stream()
                                .map(itemRequest -> createRecipeItem(menuItem, itemRequest))
                                .toList();

                List<RecipeItem> savedRecipeItems = recipeItemRepository.saveAll(newRecipeItems);

                eventPublisher.publishEvent(new InventoryChangeEvent("recipe_updated", menuItemId));
                eventPublisher.publishEvent(new MenuChangeEvent("recipe_updated", menuItemId));

                return recipeMapper.toResponses(savedRecipeItems);
        }

        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = CacheNames.INVENTORY, allEntries = true),
                        @CacheEvict(value = CacheNames.PUBLIC_MENU, allEntries = true),
                        @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
                        @CacheEvict(value = CacheNames.AI_MENU_CONTEXT, allEntries = true)
        })
        public void deleteRecipe(@NonNull Long menuItemId) {
                getMenuItem(menuItemId);

                List<RecipeItem> recipeItems = recipeItemRepository.findByMenuItemId(menuItemId);

                if (!recipeItems.isEmpty()) {
                        recipeItemRepository.deleteAll(recipeItems);
                }

                eventPublisher.publishEvent(new InventoryChangeEvent("recipe_deleted", menuItemId));
                eventPublisher.publishEvent(new MenuChangeEvent("recipe_deleted", menuItemId));
        }

        private RecipeItem createRecipeItem(
                        MenuItem menuItem,
                        RecipeItemRequest request) {
                InventoryItem inventoryItem = getActiveInventoryItem(request.inventoryItemId());

                return recipeMapper.toEntity(
                                menuItem,
                                inventoryItem,
                                request);
        }

        private MenuItem getMenuItem(Long menuItemId) {
                return menuItemRepository.findById(menuItemId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));
        }

        private InventoryItem getActiveInventoryItem(Long inventoryItemId) {
                return inventoryItemRepository.findById(inventoryItemId)
                                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_ITEM_NOT_FOUND));
        }

        private void validateDuplicateInventoryItems(List<RecipeItemRequest> items) {
                if (items == null) {
                        throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }

                Set<Long> inventoryItemIds = new LinkedHashSet<>();

                for (RecipeItemRequest item : items) {
                        if (!inventoryItemIds.add(item.inventoryItemId())) {
                                throw new BusinessException(ErrorCode.RECIPE_ITEM_DUPLICATED);
                        }
                }
        }
}
