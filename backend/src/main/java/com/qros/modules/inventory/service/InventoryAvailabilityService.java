package com.qros.modules.inventory.service;

import com.qros.modules.inventory.dto.internal.InventoryRequirement;
import com.qros.modules.inventory.dto.internal.InventoryReservationResult;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryAvailabilityService {

    private static final int QUANTITY_SCALE = 3;

    private final RecipeItemRepository recipeItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final ComboRepository comboRepository;

    @Transactional(readOnly = true)
    public InventoryReservationResult checkMenuItemAvailability(
            @NonNull Long menuItemId,
            @NonNull BigDecimal quantity) {
        if (!menuItemRepository.existsById(menuItemId)) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND);
        }

        BigDecimal safeQuantity = normalizePositiveQuantity(quantity);

        List<RecipeItem> recipeItems = recipeItemRepository.findByMenuItemId(menuItemId);

        List<InventoryRequirement> requirements = recipeItems.stream()
                .map(recipeItem -> toRequirement(recipeItem, safeQuantity))
                .toList();

        boolean success = requirements.stream()
                .allMatch(InventoryRequirement::sufficient);

        return new InventoryReservationResult(
                null,
                success,
                requirements);
    }

    @Transactional(readOnly = true)
    public boolean canPrepareMenuItem(
            @NonNull Long menuItemId,
            @NonNull BigDecimal quantity) {
        return checkMenuItemAvailability(menuItemId, quantity).success();
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> getMenuItemAvailability(List<Long> menuItemIds) {
        if (menuItemIds == null || menuItemIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> quantitiesByMenuItem = menuItemIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> BigDecimal.ONE));

        return getMenuItemAvailability(quantitiesByMenuItem);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> getComboAvailabilityByIds(List<Long> comboIds) {
        if (comboIds == null || comboIds.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = comboIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        List<Combo> combos = comboRepository.findAllByIdInWithItems(ids);

        List<Long> menuItemIds = combos.stream()
                .filter(combo -> combo.getItems() != null)
                .flatMap(combo -> combo.getItems().stream())
                .map(item -> item.getMenuItem() == null ? null : item.getMenuItem().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<RecipeItem>> recipesByMenuItem = recipesByMenuItem(menuItemIds);

        return combos.stream()
                .collect(Collectors.toMap(
                        Combo::getId,
                        combo -> canPrepareCombo(combo, recipesByMenuItem)));
    }

    private Map<Long, Boolean> getMenuItemAvailability(Map<Long, BigDecimal> quantitiesByMenuItem) {
        if (quantitiesByMenuItem.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<RecipeItem>> recipesByMenuItem = recipesByMenuItem(quantitiesByMenuItem.keySet().stream().toList());
        Map<Long, Boolean> availability = new HashMap<>();

        quantitiesByMenuItem.forEach((menuItemId, quantity) ->
                availability.put(menuItemId, canPrepareMenuItem(menuItemId, quantity, recipesByMenuItem)));

        return availability;
    }

    private Map<Long, List<RecipeItem>> recipesByMenuItem(List<Long> menuItemIds) {
        if (menuItemIds == null || menuItemIds.isEmpty()) {
            return Map.of();
        }

        return recipeItemRepository.findByMenuItemIds(menuItemIds).stream()
                .collect(Collectors.groupingBy(recipeItem -> recipeItem.getMenuItem().getId()));
    }

    private boolean canPrepareCombo(Combo combo, Map<Long, List<RecipeItem>> recipesByMenuItem) {
        if (combo.getItems() == null || combo.getItems().isEmpty()) {
            return true;
        }

        return combo.getItems().stream()
                .filter(item -> item.getMenuItem() != null)
                .allMatch(item -> canPrepareMenuItem(
                        item.getMenuItem().getId(),
                        quantityFrom(item.getQuantity()),
                        recipesByMenuItem));
    }

    private boolean canPrepareMenuItem(
            Long menuItemId,
            BigDecimal quantity,
            Map<Long, List<RecipeItem>> recipesByMenuItem) {
        List<RecipeItem> recipeItems = recipesByMenuItem.getOrDefault(menuItemId, List.of());

        return recipeItems.stream()
                .map(recipeItem -> toRequirement(recipeItem, quantity))
                .allMatch(InventoryRequirement::sufficient);
    }

    private InventoryRequirement toRequirement(
            RecipeItem recipeItem,
            BigDecimal menuItemQuantity) {
        InventoryItem inventoryItem = recipeItem.getInventoryItem();

        BigDecimal requiredQuantity = normalizeQuantity(recipeItem.getQuantityRequired())
                .multiply(menuItemQuantity)
                .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);

        BigDecimal availableQuantity = inventoryItem.availableQuantity();

        boolean sufficient = Boolean.TRUE.equals(inventoryItem.getActive())
                && availableQuantity.compareTo(requiredQuantity) >= 0;

        return new InventoryRequirement(
                inventoryItem.getId(),
                inventoryItem.getName(),
                inventoryItem.getUnit(),
                requiredQuantity,
                availableQuantity,
                sufficient);
    }

    private BigDecimal normalizePositiveQuantity(BigDecimal value) {
        BigDecimal quantity = normalizeQuantity(value);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }

        return quantity;
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }

        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal quantityFrom(Integer value) {
        return BigDecimal.valueOf(value == null ? 1 : value)
                .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }
}
