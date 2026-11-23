package com.qros.modules.inventory.service;

import com.qros.modules.inventory.dto.internal.InventoryRequirement;
import com.qros.modules.inventory.dto.internal.InventoryReservationResult;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.event.DomainEvents.ComboChangeEvent;
import com.qros.shared.event.DomainEvents.MenuChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryAvailabilityService {

    private static final int QUANTITY_SCALE = 3;

    private final RecipeItemRepository recipeItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final ComboRepository comboRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public InventoryReservationResult checkMenuItemAvailability(
            @NonNull Long menuItemId, @NonNull BigDecimal quantity) {
        if (!menuItemRepository.existsById(menuItemId)) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND);
        }

        BigDecimal safeQuantity = normalizePositiveQuantity(quantity);

        List<RecipeItem> recipeItems = recipeItemRepository.findByMenuItemId(menuItemId);

        List<InventoryRequirement> requirements = recipeItems.stream()
                .map(recipeItem -> toRequirement(recipeItem, safeQuantity))
                .toList();

        boolean success = requirements.stream().allMatch(InventoryRequirement::sufficient);

        return new InventoryReservationResult(null, success, requirements);
    }

    @Transactional(readOnly = true)
    public boolean canPrepareMenuItem(@NonNull Long menuItemId, @NonNull BigDecimal quantity) {
        return checkMenuItemAvailability(menuItemId, quantity).success();
    }

    @Transactional(readOnly = true)
    public boolean canPrepareCombo(@NonNull Combo combo) {
        List<Long> menuItemIds = extractComboMenuItemIds(List.of(combo));
        Map<Long, List<RecipeItem>> recipesByMenuItem = recipesByMenuItem(menuItemIds);
        return canPrepareCombo(combo, recipesByMenuItem);
    }

    @Transactional(readOnly = true)
    public boolean hasSufficientInventoryForCombo(@NonNull Combo combo) {
        List<Long> menuItemIds = extractComboMenuItemIds(List.of(combo));
        Map<Long, List<RecipeItem>> recipesByMenuItem = recipesByMenuItem(menuItemIds);
        return hasSufficientInventoryForCombo(combo, recipesByMenuItem);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> getMenuItemAvailability(List<Long> menuItemIds) {
        if (menuItemIds == null || menuItemIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> quantitiesByMenuItem = menuItemIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> BigDecimal.ONE));

        return getMenuItemAvailability(quantitiesByMenuItem);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> getComboAvailabilityByIds(List<Long> comboIds) {
        if (comboIds == null || comboIds.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = comboIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        List<Combo> combos = comboRepository.findAllByIdInWithItems(ids);
        List<Long> menuItemIds = extractComboMenuItemIds(combos);

        Map<Long, List<RecipeItem>> recipesByMenuItem = recipesByMenuItem(menuItemIds);

        return combos.stream()
                .collect(Collectors.toMap(Combo::getId, combo -> canPrepareCombo(combo, recipesByMenuItem)));
    }

    @Transactional
    public void syncCombosForMenuItem(@NonNull MenuItem menuItem) {
        if (menuItem.getId() == null) {
            return;
        }

        List<Combo> affectedCombos = comboRepository.findAllByMenuItemIdsWithItems(List.of(menuItem.getId()));
        if (affectedCombos.isEmpty()) {
            return;
        }

        boolean menuItemOrderable = Boolean.TRUE.equals(menuItem.getActive())
                && Boolean.TRUE.equals(menuItem.getAvailable())
                && canPrepareMenuItem(menuItem.getId(), BigDecimal.ONE);

        List<Long> allComboMenuItemIds = extractComboMenuItemIds(affectedCombos);
        Map<Long, List<RecipeItem>> recipesByMenuItem = recipesByMenuItem(allComboMenuItemIds);

        for (Combo combo : affectedCombos) {
            if (!menuItemOrderable) {
                if (Boolean.TRUE.equals(combo.getAvailable())) {
                    combo.setAvailable(false);
                    comboRepository.save(combo);
                    eventPublisher.publishEvent(new ComboChangeEvent("availability_updated", combo.getId()));
                }
            } else {
                boolean canPrepare = canPrepareCombo(combo, recipesByMenuItem);
                if (canPrepare && !Boolean.TRUE.equals(combo.getAvailable())) {
                    combo.setAvailable(true);
                    comboRepository.save(combo);
                    eventPublisher.publishEvent(new ComboChangeEvent("availability_updated", combo.getId()));
                } else if (!canPrepare && Boolean.TRUE.equals(combo.getAvailable())) {
                    combo.setAvailable(false);
                    comboRepository.save(combo);
                    eventPublisher.publishEvent(new ComboChangeEvent("availability_updated", combo.getId()));
                }
            }
        }
    }

    @Transactional
    public void syncAvailabilityForInventoryItems(
            Collection<Long> inventoryItemIds, boolean allowAutoEnableWhenSufficient) {
        if (inventoryItemIds == null || inventoryItemIds.isEmpty()) {
            return;
        }

        List<Long> validInventoryIds =
                inventoryItemIds.stream().filter(Objects::nonNull).distinct().toList();
        if (validInventoryIds.isEmpty()) {
            return;
        }

        List<Long> affectedMenuItemIds = recipeItemRepository.findByInventoryItemIds(validInventoryIds).stream()
                .map(recipeItem -> recipeItem.getMenuItem() == null
                        ? null
                        : recipeItem.getMenuItem().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (affectedMenuItemIds.isEmpty()) {
            return;
        }

        syncAvailabilityForMenuItems(affectedMenuItemIds, allowAutoEnableWhenSufficient);
    }

    @Transactional
    public void syncAvailabilityForMenuItems(Collection<Long> menuItemIds, boolean allowAutoEnableWhenSufficient) {
        if (menuItemIds == null || menuItemIds.isEmpty()) {
            return;
        }

        List<Long> validMenuItemIds =
                menuItemIds.stream().filter(Objects::nonNull).distinct().toList();
        if (validMenuItemIds.isEmpty()) {
            return;
        }

        List<MenuItem> menuItems = menuItemRepository.findAllByIdIn(validMenuItemIds);
        Map<Long, List<RecipeItem>> recipesByMenuItem = recipesByMenuItem(validMenuItemIds);

        for (MenuItem menuItem : menuItems) {
            boolean hasStock = canPrepareMenuItem(menuItem.getId(), BigDecimal.ONE, recipesByMenuItem);
            if (!hasStock && Boolean.TRUE.equals(menuItem.getAvailable())) {
                menuItem.setAvailable(false);
                menuItemRepository.save(menuItem);
                eventPublisher.publishEvent(new MenuChangeEvent("availability_updated", menuItem.getId()));
            } else if (hasStock && allowAutoEnableWhenSufficient && !Boolean.TRUE.equals(menuItem.getAvailable())) {
                menuItem.setAvailable(true);
                menuItemRepository.save(menuItem);
                eventPublisher.publishEvent(new MenuChangeEvent("availability_updated", menuItem.getId()));
            }
        }

        List<Combo> affectedCombos = comboRepository.findAllByMenuItemIdsWithItems(validMenuItemIds);
        if (affectedCombos.isEmpty()) {
            return;
        }

        List<Long> allComboMenuItemIds = extractComboMenuItemIds(affectedCombos);
        Map<Long, List<RecipeItem>> comboRecipesByMenuItem = recipesByMenuItem(allComboMenuItemIds);

        for (Combo combo : affectedCombos) {
            boolean canPrepare = canPrepareCombo(combo, comboRecipesByMenuItem);
            if (!canPrepare && Boolean.TRUE.equals(combo.getAvailable())) {
                combo.setAvailable(false);
                comboRepository.save(combo);
                eventPublisher.publishEvent(new ComboChangeEvent("availability_updated", combo.getId()));
            } else if (canPrepare && allowAutoEnableWhenSufficient && !Boolean.TRUE.equals(combo.getAvailable())) {
                combo.setAvailable(true);
                comboRepository.save(combo);
                eventPublisher.publishEvent(new ComboChangeEvent("availability_updated", combo.getId()));
            }
        }
    }

    public boolean areAllComboItemsOrderable(Combo combo) {
        if (combo == null) {
            return false;
        }
        if (combo.getItems() == null || combo.getItems().isEmpty()) {
            return true;
        }

        List<ComboItem> validItems = combo.getItems().stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.isDeleted())
                .toList();

        if (validItems.isEmpty()) {
            return true;
        }

        return validItems.stream().allMatch(item -> {
            MenuItem menuItem = item.getMenuItem();
            return menuItem != null
                    && !menuItem.isDeleted()
                    && Boolean.TRUE.equals(menuItem.getActive())
                    && Boolean.TRUE.equals(menuItem.getAvailable());
        });
    }

    private List<Long> extractComboMenuItemIds(List<Combo> combos) {
        return combos.stream()
                .filter(Objects::nonNull)
                .filter(combo -> combo.getItems() != null)
                .flatMap(combo -> combo.getItems().stream())
                .filter(Objects::nonNull)
                .filter(item -> !item.isDeleted())
                .map(item ->
                        item.getMenuItem() == null ? null : item.getMenuItem().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, Boolean> getMenuItemAvailability(Map<Long, BigDecimal> quantitiesByMenuItem) {
        if (quantitiesByMenuItem.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<RecipeItem>> recipesByMenuItem =
                recipesByMenuItem(quantitiesByMenuItem.keySet().stream().toList());
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
                .collect(Collectors.groupingBy(
                        recipeItem -> recipeItem.getMenuItem().getId()));
    }

    private boolean canPrepareCombo(Combo combo, Map<Long, List<RecipeItem>> recipesByMenuItem) {
        if (!areAllComboItemsOrderable(combo)) {
            return false;
        }

        return hasSufficientInventoryForCombo(combo, recipesByMenuItem);
    }

    private boolean hasSufficientInventoryForCombo(Combo combo, Map<Long, List<RecipeItem>> recipesByMenuItem) {
        if (combo == null || combo.getItems() == null || combo.getItems().isEmpty()) {
            return true;
        }

        Map<Long, BigDecimal> requiredByInventoryId = new HashMap<>();
        Map<Long, InventoryItem> inventoryItemsById = new HashMap<>();

        for (ComboItem comboItem : combo.getItems()) {
            if (comboItem == null || comboItem.isDeleted() || comboItem.getMenuItem() == null) {
                continue;
            }

            Long menuItemId = comboItem.getMenuItem().getId();
            if (menuItemId == null) {
                continue;
            }

            BigDecimal comboItemQuantity = quantityFrom(comboItem.getQuantity());
            List<RecipeItem> recipeItems = recipesByMenuItem.getOrDefault(menuItemId, List.of());

            for (RecipeItem recipeItem : recipeItems) {
                InventoryItem inventoryItem = recipeItem.getInventoryItem();
                if (inventoryItem == null
                        || inventoryItem.isDeleted()
                        || !Boolean.TRUE.equals(inventoryItem.getActive())) {
                    return false;
                }

                BigDecimal requiredQuantity = normalizeQuantity(recipeItem.getQuantityRequired())
                        .multiply(comboItemQuantity)
                        .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);

                requiredByInventoryId.merge(
                        inventoryItem.getId(),
                        requiredQuantity,
                        (a, b) -> a.add(b).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
                inventoryItemsById.putIfAbsent(inventoryItem.getId(), inventoryItem);
            }
        }

        for (Map.Entry<Long, BigDecimal> entry : requiredByInventoryId.entrySet()) {
            InventoryItem inventoryItem = inventoryItemsById.get(entry.getKey());
            if (inventoryItem == null
                    || !Boolean.TRUE.equals(inventoryItem.getActive())
                    || inventoryItem.availableQuantity().compareTo(entry.getValue()) < 0) {
                return false;
            }
        }

        return true;
    }

    private boolean canPrepareMenuItem(
            Long menuItemId, BigDecimal quantity, Map<Long, List<RecipeItem>> recipesByMenuItem) {
        List<RecipeItem> recipeItems = recipesByMenuItem.getOrDefault(menuItemId, List.of());

        return recipeItems.stream()
                .map(recipeItem -> toRequirement(recipeItem, quantity))
                .allMatch(InventoryRequirement::sufficient);
    }

    private InventoryRequirement toRequirement(RecipeItem recipeItem, BigDecimal menuItemQuantity) {
        InventoryItem inventoryItem = recipeItem.getInventoryItem();
        if (inventoryItem == null || inventoryItem.isDeleted()) {
            return new InventoryRequirement(null, "", "", BigDecimal.ZERO, BigDecimal.ZERO, false);
        }

        BigDecimal requiredQuantity = normalizeQuantity(recipeItem.getQuantityRequired())
                .multiply(menuItemQuantity)
                .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);

        BigDecimal availableQuantity = inventoryItem.availableQuantity();

        boolean sufficient =
                Boolean.TRUE.equals(inventoryItem.getActive()) && availableQuantity.compareTo(requiredQuantity) >= 0;

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
        return BigDecimal.valueOf(value == null ? 1 : value).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }
}
