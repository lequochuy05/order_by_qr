package com.qros.modules.inventory.service;

import com.qros.modules.inventory.dto.internal.InventoryRequirement;
import com.qros.modules.inventory.dto.internal.InventoryReservationResult;
import com.qros.modules.inventory.mapper.InventoryReservationMapper;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.OrderItemInventoryReservation;
import com.qros.modules.inventory.model.RecipeItem;
import com.qros.modules.inventory.model.enums.InventoryReservationStatus;
import com.qros.modules.inventory.model.enums.StockMovementType;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.OrderItemInventoryReservationRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.modules.menu.model.MenuItem;
import org.springframework.context.ApplicationEventPublisher;
import com.qros.shared.event.DomainEvents.*;
import com.qros.modules.order.model.OrderItem;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    private static final int QUANTITY_SCALE = 3;

    private final InventoryItemRepository inventoryItemRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final OrderItemInventoryReservationRepository reservationRepository;
    private final InventoryReservationMapper reservationMapper;
    private final StockMovementService stockMovementService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InventoryReservationResult reserveForOrderItem(@NonNull OrderItem orderItem) {
        return reserveForOrderItem(orderItem, orderItem.getQuantity(), true);
    }

    @Transactional
    public InventoryReservationResult reserveForOrderItem(
            @NonNull OrderItem orderItem,
            @NonNull Integer quantity) {
        return reserveForOrderItem(orderItem, quantity, false);
    }

    @Transactional
    public void reserveForOrderItems(@NonNull Map<OrderItem, Integer> quantitiesToReserve) {
        if (quantitiesToReserve.isEmpty()) {
            return;
        }

        List<ReservationDraft> allDrafts = new ArrayList<>();
        Map<OrderItem, List<ReservationDraft>> itemDraftsMap = new LinkedHashMap<>();

        // 1. Gather all unique menu item IDs across all order items (including combo items)
        Set<Long> menuItemIds = quantitiesToReserve.keySet().stream()
                .filter(Objects::nonNull)
                .flatMap(item -> {
                    List<MenuItem> menuItems = new ArrayList<>();
                    if (item.getMenuItem() != null) {
                        menuItems.add(item.getMenuItem());
                    }
                    if (item.getCombo() != null && item.getCombo().getItems() != null) {
                        item.getCombo().getItems().stream()
                                .filter(ci -> ci.getMenuItem() != null)
                                .forEach(ci -> menuItems.add(ci.getMenuItem()));
                    }
                    return menuItems.stream();
                })
                .map(MenuItem::getId)
                .collect(Collectors.toSet());

        if (menuItemIds.isEmpty()) {
            return;
        }

        // 2. Fetch all required recipe items in one query
        List<RecipeItem> allRecipeItems = recipeItemRepository.findByMenuItemIds(new ArrayList<>(menuItemIds));
        Map<Long, List<RecipeItem>> recipeItemsByMenuItem = allRecipeItems.stream()
                .collect(Collectors.groupingBy(r -> r.getMenuItem().getId()));

        // 3. Build drafts using pre-fetched recipe items
        for (Map.Entry<OrderItem, Integer> entry : quantitiesToReserve.entrySet()) {
            OrderItem orderItem = entry.getKey();
            BigDecimal orderItemQuantity = quantityFrom(entry.getValue());
            if (orderItemQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Map<Long, ReservationDraft> drafts = new LinkedHashMap<>();

            if (orderItem.getMenuItem() != null) {
                mergeMenuItemRequirementsFast(
                        drafts,
                        orderItem.getMenuItem().getId(),
                        orderItemQuantity,
                        recipeItemsByMenuItem);
            }

            if (orderItem.getCombo() != null && orderItem.getCombo().getItems() != null) {
                for (var comboItem : orderItem.getCombo().getItems()) {
                    if (comboItem.getMenuItem() == null) {
                        continue;
                    }

                    BigDecimal comboItemQuantity = quantityFrom(comboItem.getQuantity());
                    BigDecimal multiplier = orderItemQuantity.multiply(comboItemQuantity);

                    mergeMenuItemRequirementsFast(
                            drafts,
                            comboItem.getMenuItem().getId(),
                            multiplier,
                            recipeItemsByMenuItem);
                }
            }

            List<ReservationDraft> draftsList = new ArrayList<>(drafts.values());
            itemDraftsMap.put(orderItem, draftsList);
            allDrafts.addAll(draftsList);
        }

        if (allDrafts.isEmpty()) {
            return;
        }

        // 4. Aggregate required quantities per inventory item across the whole order
        Map<Long, BigDecimal> aggregatedRequirements = new LinkedHashMap<>();
        for (ReservationDraft draft : allDrafts) {
            aggregatedRequirements.merge(
                    draft.inventoryItemId(),
                    draft.requiredQuantity(),
                    (a, b) -> a.add(b).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
        }

        // 5. Sort inventory item IDs to lock in a consistent order (prevent deadlock)
        List<Long> sortedInventoryItemIds = aggregatedRequirements.keySet().stream()
                .sorted()
                .toList();

        Map<Long, InventoryItem> lockedItemsById = new LinkedHashMap<>();

        // 6. Lock and validate all required inventory items
        for (Long inventoryItemId : sortedInventoryItemIds) {
            InventoryItem lockedItem = getInventoryItemForUpdate(inventoryItemId);
            lockedItemsById.put(inventoryItemId, lockedItem);

            BigDecimal requiredQuantity = aggregatedRequirements.get(inventoryItemId);
            BigDecimal availableQuantity = lockedItem.availableQuantity();
            
            boolean sufficient = Boolean.TRUE.equals(lockedItem.getActive())
                    && availableQuantity.compareTo(requiredQuantity) >= 0;

            if (!sufficient) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
            }
        }

        // 7. Update reserved quantities and save all inventory items
        for (Map.Entry<Long, BigDecimal> entry : aggregatedRequirements.entrySet()) {
            InventoryItem lockedItem = lockedItemsById.get(entry.getKey());
            BigDecimal requiredQuantity = entry.getValue();

            BigDecimal reservedQuantity = normalizeQuantity(lockedItem.getReservedQuantity())
                    .add(requiredQuantity);
            lockedItem.setReservedQuantity(reservedQuantity);
        }
        inventoryItemRepository.saveAll(lockedItemsById.values());

        // 8. Create and associate reservations for each order item
        for (Map.Entry<OrderItem, List<ReservationDraft>> entry : itemDraftsMap.entrySet()) {
            OrderItem orderItem = entry.getKey();
            for (ReservationDraft draft : entry.getValue()) {
                InventoryItem lockedItem = lockedItemsById.get(draft.inventoryItemId());

                OrderItemInventoryReservation reservation = reservationMapper.toEntity(
                        orderItem,
                        lockedItem,
                        draft.requiredQuantity());

                upsertReservation(orderItem, reservation);
            }
            if (orderItem.getId() != null) {
                eventPublisher.publishEvent(new InventoryChangeEvent("inventory_reserved", orderItem.getId()));
            }
        }
    }

    private InventoryReservationResult reserveForOrderItem(
            OrderItem orderItem,
            Number quantity,
            boolean skipWhenAlreadyReserved) {
        Long orderItemId = orderItem.getId();

        if (skipWhenAlreadyReserved
                && orderItemId != null
                && reservationRepository.existsByOrderItem_IdAndStatus(
                orderItemId,
                InventoryReservationStatus.RESERVED)) {
            return new InventoryReservationResult(
                    orderItemId,
                    true,
                    List.of());
        }

        BigDecimal orderItemQuantity = quantityFrom(quantity);

        if (orderItemQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }

        List<ReservationDraft> drafts = buildReservationDrafts(orderItem, orderItemQuantity);

        if (drafts.isEmpty()) {
            return new InventoryReservationResult(
                    orderItemId,
                    true,
                    List.of());
        }

        List<ReservationDraft> sortedDrafts = drafts.stream()
                .sorted(Comparator.comparing(draft -> draft.inventoryItemId))
                .toList();

        List<InventoryRequirement> requirements = new ArrayList<>();
        Map<Long, InventoryItem> lockedItemsById = new LinkedHashMap<>();

        for (ReservationDraft draft : sortedDrafts) {
            InventoryItem lockedItem = getInventoryItemForUpdate(draft.inventoryItemId());
            lockedItemsById.put(draft.inventoryItemId(), lockedItem);

            BigDecimal availableQuantity = lockedItem.availableQuantity();
            boolean sufficient = Boolean.TRUE.equals(lockedItem.getActive())
                    && availableQuantity.compareTo(draft.requiredQuantity()) >= 0;

            requirements.add(new InventoryRequirement(
                    lockedItem.getId(),
                    lockedItem.getName(),
                    lockedItem.getUnit(),
                    draft.requiredQuantity(),
                    availableQuantity,
                    sufficient));

            if (!sufficient) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
            }
        }

        for (ReservationDraft draft : sortedDrafts) {
            InventoryItem lockedItem = lockedItemsById.get(draft.inventoryItemId());

            BigDecimal reservedQuantity = normalizeQuantity(lockedItem.getReservedQuantity())
                    .add(draft.requiredQuantity());

            lockedItem.setReservedQuantity(reservedQuantity);

            inventoryItemRepository.save(lockedItem);

            OrderItemInventoryReservation reservation = reservationMapper.toEntity(
                    orderItem,
                    lockedItem,
                    draft.requiredQuantity());

            upsertReservation(orderItem, reservation);
        }

        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_reserved", orderItemId));

        return new InventoryReservationResult(
                orderItemId,
                true,
                requirements);
    }

    @Transactional
    public void releaseForOrderItem(@NonNull OrderItem orderItem) {
        releaseForOrderItem(orderItem.getId());
    }

    @Transactional
    public void restoreOrderItem(@NonNull OrderItem orderItem) {
        releaseForOrderItem(orderItem);
    }

    @Transactional
    public void adjustReservationForQuantity(
            @NonNull OrderItem orderItem,
            int oldQuantity,
            int newQuantity) {
        if (newQuantity <= 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }

        if (oldQuantity == newQuantity) {
            return;
        }

        releaseForOrderItem(orderItem);
        reserveForOrderItem(orderItem, newQuantity);
    }

    private void releaseForOrderItem(Long orderItemId) {
        List<OrderItemInventoryReservation> reservations = reservationRepository.findByOrderItemIdAndStatusForUpdate(
                orderItemId,
                InventoryReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            return;
        }

        for (OrderItemInventoryReservation reservation : reservations) {
            InventoryItem item = getInventoryItemForUpdate(
                    reservation.getInventoryItem().getId());

            BigDecimal newReservedQuantity = normalizeQuantity(item.getReservedQuantity())
                    .subtract(normalizeQuantity(reservation.getReservedQuantity()));

            if (newReservedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                newReservedQuantity = BigDecimal.ZERO.setScale(
                        QUANTITY_SCALE,
                        RoundingMode.HALF_UP);
            }

            item.setReservedQuantity(newReservedQuantity);

            reservation.setStatus(InventoryReservationStatus.RELEASED);
            reservation.setReleasedAt(AppTime.now());

            inventoryItemRepository.save(item);
            reservationRepository.save(reservation);
        }

        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_released", orderItemId));
    }

    @Transactional
    public void consumeForOrderItem(@NonNull OrderItem orderItem) {
        Long orderItemId = orderItem.getId();

        List<OrderItemInventoryReservation> reservations = reservationRepository.findByOrderItemIdAndStatusForUpdate(
                orderItemId,
                InventoryReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            return;
        }

        for (OrderItemInventoryReservation reservation : reservations) {
            InventoryItem item = getInventoryItemForUpdate(
                    reservation.getInventoryItem().getId());

            BigDecimal consumedQuantity = normalizeQuantity(reservation.getReservedQuantity());

            BigDecimal quantityBefore = normalizeQuantity(item.getQuantityOnHand());
            BigDecimal quantityAfter = quantityBefore.subtract(consumedQuantity);

            if (quantityAfter.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
            }

            BigDecimal newReservedQuantity = normalizeQuantity(item.getReservedQuantity())
                    .subtract(consumedQuantity);

            if (newReservedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                newReservedQuantity = BigDecimal.ZERO.setScale(
                        QUANTITY_SCALE,
                        RoundingMode.HALF_UP);
            }

            item.setQuantityOnHand(quantityAfter);
            item.setReservedQuantity(newReservedQuantity);

            reservation.setStatus(InventoryReservationStatus.CONSUMED);
            reservation.setConsumedAt(AppTime.now());

            inventoryItemRepository.save(item);
            reservationRepository.save(reservation);

            stockMovementService.recordMovement(
                    item,
                    orderItem,
                    StockMovementType.CONSUME,
                    consumedQuantity,
                    quantityBefore,
                    quantityAfter,
                    "Consume inventory for order item #" + orderItemId);
        }

        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_consumed", orderItemId));
    }

    private List<ReservationDraft> buildReservationDrafts(
            OrderItem orderItem,
            BigDecimal orderItemQuantity) {
        Map<Long, ReservationDraft> drafts = new LinkedHashMap<>();

        if (orderItem.getMenuItem() != null) {
            mergeMenuItemRequirements(
                    drafts,
                    orderItem.getMenuItem(),
                    orderItemQuantity);
        }

        if (orderItem.getCombo() != null && orderItem.getCombo().getItems() != null) {
            for (var comboItem : orderItem.getCombo().getItems()) {
                if (comboItem.getMenuItem() == null) {
                    continue;
                }

                BigDecimal comboItemQuantity = quantityFrom(comboItem.getQuantity());
                BigDecimal multiplier = orderItemQuantity.multiply(comboItemQuantity);

                mergeMenuItemRequirements(
                        drafts,
                        comboItem.getMenuItem(),
                        multiplier);
            }
        }

        return new ArrayList<>(drafts.values());
    }

    private void upsertReservation(
            OrderItem orderItem,
            OrderItemInventoryReservation newReservation) {
        OrderItemInventoryReservation existingReservation = findExistingReservedReservation(
                orderItem,
                newReservation.getInventoryItem().getId());

        if (existingReservation != null) {
            existingReservation.setReservedQuantity(
                    normalizeQuantity(existingReservation.getReservedQuantity())
                            .add(normalizeQuantity(newReservation.getReservedQuantity())));

            if (existingReservation.getId() != null) {
                reservationRepository.save(existingReservation);
            }

            return;
        }

        if (orderItem.getId() == null) {
            orderItem.addInventoryReservation(newReservation);
            return;
        }

        reservationRepository.save(newReservation);
    }

    private OrderItemInventoryReservation findExistingReservedReservation(
            OrderItem orderItem,
            Long inventoryItemId) {
        if (orderItem.getId() != null) {
            return reservationRepository
                    .findByOrderItem_IdAndInventoryItem_IdAndStatus(
                            orderItem.getId(),
                            inventoryItemId,
                            InventoryReservationStatus.RESERVED)
                    .orElse(null);
        }

        return orderItem.getInventoryReservations().stream()
                .filter(reservation -> reservation.getStatus() == InventoryReservationStatus.RESERVED)
                .filter(reservation -> reservation.getInventoryItem() != null)
                .filter(reservation -> inventoryItemId.equals(reservation.getInventoryItem().getId()))
                .findFirst()
                .orElse(null);
    }

    private void mergeMenuItemRequirements(
            Map<Long, ReservationDraft> drafts,
            MenuItem menuItem,
            BigDecimal multiplier) {
        List<RecipeItem> recipeItems = recipeItemRepository.findByMenuItemId(menuItem.getId());

        for (RecipeItem recipeItem : recipeItems) {
            InventoryItem inventoryItem = recipeItem.getInventoryItem();

            if (inventoryItem == null || inventoryItem.getId() == null) {
                continue;
            }

            BigDecimal requiredQuantity = normalizeQuantity(recipeItem.getQuantityRequired())
                    .multiply(multiplier)
                    .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);

            drafts.merge(
                    inventoryItem.getId(),
                    new ReservationDraft(inventoryItem.getId(), requiredQuantity),
                    (oldValue, newValue) -> new ReservationDraft(
                            oldValue.inventoryItemId(),
                            oldValue.requiredQuantity().add(newValue.requiredQuantity())
                                    .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP)));
        }
    }

    private void mergeMenuItemRequirementsFast(
            Map<Long, ReservationDraft> drafts,
            Long menuItemId,
            BigDecimal multiplier,
            Map<Long, List<RecipeItem>> recipeItemsByMenuItem) {
        List<RecipeItem> recipeItems = recipeItemsByMenuItem.getOrDefault(menuItemId, List.of());

        for (RecipeItem recipeItem : recipeItems) {
            InventoryItem inventoryItem = recipeItem.getInventoryItem();

            if (inventoryItem == null || inventoryItem.getId() == null) {
                continue;
            }

            BigDecimal requiredQuantity = normalizeQuantity(recipeItem.getQuantityRequired())
                    .multiply(multiplier)
                    .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);

            drafts.merge(
                    inventoryItem.getId(),
                    new ReservationDraft(inventoryItem.getId(), requiredQuantity),
                    (oldValue, newValue) -> new ReservationDraft(
                            oldValue.inventoryItemId(),
                            oldValue.requiredQuantity().add(newValue.requiredQuantity())
                                    .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP)));
        }
    }

    private InventoryItem getInventoryItemForUpdate(Long inventoryItemId) {
        return inventoryItemRepository.findByIdForUpdate(inventoryItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_ITEM_NOT_FOUND));
    }

    private BigDecimal quantityFrom(Number value) {
        if (value == null) {
            return BigDecimal.ONE.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }

        if (value instanceof BigDecimal decimalValue) {
            return normalizeQuantity(decimalValue);
        }

        return BigDecimal.valueOf(value.doubleValue())
                .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }

        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private record ReservationDraft(
            Long inventoryItemId,
            BigDecimal requiredQuantity) {
    }
}
