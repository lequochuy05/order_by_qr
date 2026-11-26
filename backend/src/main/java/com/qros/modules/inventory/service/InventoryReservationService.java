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
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.service.OrderInventoryReservationPort;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryReservationService implements OrderInventoryReservationPort {

    private static final int QUANTITY_SCALE = 3;

    private final InventoryItemRepository inventoryItemRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final OrderItemInventoryReservationRepository reservationRepository;
    private final InventoryReservationMapper reservationMapper;
    private final StockMovementService stockMovementService;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InventoryReservationResult reserveForOrderItem(@NonNull OrderItem orderItem) {
        return reserveForOrderItem(orderItem, orderItem.getQuantity(), true);
    }

    @Transactional
    public InventoryReservationResult reserveForOrderItem(@NonNull OrderItem orderItem, @NonNull Integer quantity) {
        return reserveForOrderItem(orderItem, quantity, false);
    }

    @Override
    @Transactional
    public void reserveForOrderItemQuantity(@NonNull OrderItem orderItem, @NonNull Integer quantity) {
        reserveForOrderItem(orderItem, quantity, false);
    }

    @Transactional
    public void reserveForOrderItems(@NonNull Map<OrderItem, Integer> quantitiesToReserve) {
        if (quantitiesToReserve.isEmpty()) {
            return;
        }

        Set<Long> menuItemIds = collectMenuItemIds(quantitiesToReserve.keySet());
        if (menuItemIds.isEmpty()) {
            return;
        }

        Map<Long, List<RecipeItem>> recipeItemsByMenuItem =
                recipeItemRepository.findByMenuItemIds(new ArrayList<>(menuItemIds)).stream()
                        .collect(Collectors.groupingBy(r -> r.getMenuItem().getId()));

        Map<OrderItem, List<ReservationDraft>> itemDraftsMap =
                buildDraftsByOrderItem(quantitiesToReserve, recipeItemsByMenuItem);
        Map<Long, BigDecimal> aggregatedRequirements = aggregateRequirements(itemDraftsMap);
        if (aggregatedRequirements.isEmpty()) {
            return;
        }

        Map<Long, InventoryItem> lockedItemsById = lockAndValidateSufficientStock(aggregatedRequirements);
        applyReservedStock(lockedItemsById, aggregatedRequirements);
        upsertReservationsForOrderItems(itemDraftsMap, lockedItemsById);

        Object eventId = itemDraftsMap.size() == 1
                        && itemDraftsMap.keySet().iterator().next().getId() != null
                ? itemDraftsMap.keySet().iterator().next().getId()
                : "batch";
        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_reserved", eventId));
    }

    private Set<Long> collectMenuItemIds(Set<OrderItem> orderItems) {
        return orderItems.stream()
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
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<OrderItem, List<ReservationDraft>> buildDraftsByOrderItem(
            Map<OrderItem, Integer> quantitiesToReserve, Map<Long, List<RecipeItem>> recipeItemsByMenuItem) {
        Map<OrderItem, List<ReservationDraft>> itemDraftsMap = new LinkedHashMap<>();
        for (Map.Entry<OrderItem, Integer> entry : quantitiesToReserve.entrySet()) {
            OrderItem orderItem = entry.getKey();
            BigDecimal orderItemQuantity = quantityFrom(entry.getValue());
            if (orderItemQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Map<Long, ReservationDraft> drafts = new LinkedHashMap<>();
            if (orderItem.getMenuItem() != null) {
                mergeMenuItemRequirementsFast(
                        drafts, orderItem.getMenuItem().getId(), orderItemQuantity, recipeItemsByMenuItem);
            }
            if (orderItem.getCombo() != null && orderItem.getCombo().getItems() != null) {
                for (var comboItem : orderItem.getCombo().getItems()) {
                    if (comboItem.getMenuItem() == null
                            || comboItem.getMenuItem().getId() == null) {
                        continue;
                    }
                    BigDecimal multiplier = orderItemQuantity.multiply(quantityFrom(comboItem.getQuantity()));
                    mergeMenuItemRequirementsFast(
                            drafts, comboItem.getMenuItem().getId(), multiplier, recipeItemsByMenuItem);
                }
            }

            if (!drafts.isEmpty()) {
                itemDraftsMap.put(orderItem, new ArrayList<>(drafts.values()));
            }
        }
        return itemDraftsMap;
    }

    private Map<Long, BigDecimal> aggregateRequirements(Map<OrderItem, List<ReservationDraft>> itemDraftsMap) {
        Map<Long, BigDecimal> aggregated = new LinkedHashMap<>();
        for (List<ReservationDraft> drafts : itemDraftsMap.values()) {
            for (ReservationDraft draft : drafts) {
                aggregated.merge(
                        draft.inventoryItemId(),
                        draft.requiredQuantity(),
                        (a, b) -> a.add(b).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
            }
        }
        return aggregated;
    }

    private Map<Long, InventoryItem> lockAndValidateSufficientStock(Map<Long, BigDecimal> aggregatedRequirements) {
        List<Long> sortedInventoryItemIds =
                aggregatedRequirements.keySet().stream().sorted().toList();
        Map<Long, InventoryItem> lockedItemsById = getInventoryItemsForUpdate(sortedInventoryItemIds);
        for (Long inventoryItemId : sortedInventoryItemIds) {
            InventoryItem lockedItem = lockedItemsById.get(inventoryItemId);
            BigDecimal requiredQuantity = aggregatedRequirements.get(inventoryItemId);
            boolean sufficient = Boolean.TRUE.equals(lockedItem.getActive())
                    && lockedItem.availableQuantity().compareTo(requiredQuantity) >= 0;
            if (!sufficient) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
            }
        }
        return lockedItemsById;
    }

    private void applyReservedStock(
            Map<Long, InventoryItem> lockedItemsById, Map<Long, BigDecimal> aggregatedRequirements) {
        for (Map.Entry<Long, BigDecimal> entry : aggregatedRequirements.entrySet()) {
            InventoryItem lockedItem = lockedItemsById.get(entry.getKey());
            BigDecimal reservedQuantity =
                    normalizeQuantity(lockedItem.getReservedQuantity()).add(entry.getValue());
            lockedItem.setReservedQuantity(reservedQuantity);
        }
        inventoryItemRepository.saveAll(lockedItemsById.values());
        inventoryAvailabilityService.syncAvailabilityForInventoryItems(lockedItemsById.keySet(), false);
    }

    private void upsertReservationsForOrderItems(
            Map<OrderItem, List<ReservationDraft>> itemDraftsMap, Map<Long, InventoryItem> lockedItemsById) {
        for (Map.Entry<OrderItem, List<ReservationDraft>> entry : itemDraftsMap.entrySet()) {
            OrderItem orderItem = entry.getKey();
            for (ReservationDraft draft : entry.getValue()) {
                InventoryItem lockedItem = lockedItemsById.get(draft.inventoryItemId());
                OrderItemInventoryReservation reservation =
                        reservationMapper.toEntity(orderItem, lockedItem, draft.requiredQuantity());
                upsertReservation(orderItem, reservation);
            }
        }
    }

    private InventoryReservationResult reserveForOrderItem(
            OrderItem orderItem, Number quantity, boolean skipWhenAlreadyReserved) {
        Long orderItemId = orderItem.getId();

        if (skipWhenAlreadyReserved
                && orderItemId != null
                && reservationRepository.existsByOrderItem_IdAndStatus(
                        orderItemId, InventoryReservationStatus.RESERVED)) {
            return new InventoryReservationResult(orderItemId, true, List.of());
        }

        BigDecimal orderItemQuantity = quantityFrom(quantity);

        if (orderItemQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }

        List<ReservationDraft> drafts = buildReservationDrafts(orderItem, orderItemQuantity);

        if (drafts.isEmpty()) {
            return new InventoryReservationResult(orderItemId, true, List.of());
        }

        List<ReservationDraft> sortedDrafts = drafts.stream()
                .sorted(Comparator.comparing(draft -> draft.inventoryItemId))
                .toList();

        List<Long> sortedInventoryItemIds =
                sortedDrafts.stream().map(ReservationDraft::inventoryItemId).toList();
        List<InventoryRequirement> requirements = new ArrayList<>();
        Map<Long, InventoryItem> lockedItemsById = getInventoryItemsForUpdate(sortedInventoryItemIds);

        for (ReservationDraft draft : sortedDrafts) {
            InventoryItem lockedItem = lockedItemsById.get(draft.inventoryItemId());
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

            BigDecimal reservedQuantity =
                    normalizeQuantity(lockedItem.getReservedQuantity()).add(draft.requiredQuantity());

            lockedItem.setReservedQuantity(reservedQuantity);

            OrderItemInventoryReservation reservation =
                    reservationMapper.toEntity(orderItem, lockedItem, draft.requiredQuantity());

            upsertReservation(orderItem, reservation);
        }
        inventoryItemRepository.saveAll(lockedItemsById.values());
        inventoryAvailabilityService.syncAvailabilityForInventoryItems(lockedItemsById.keySet(), false);

        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_reserved", orderItemId));

        return new InventoryReservationResult(orderItemId, true, requirements);
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
    public void adjustReservationForQuantity(@NonNull OrderItem orderItem, int oldQuantity, int newQuantity) {
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
                orderItemId, InventoryReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            return;
        }

        Map<Long, InventoryItem> lockedItemsById = lockReservationInventoryItems(reservations);
        var releasedAt = AppTime.now();

        for (OrderItemInventoryReservation reservation : reservations) {
            InventoryItem item =
                    lockedItemsById.get(reservation.getInventoryItem().getId());

            BigDecimal newReservedQuantity = normalizeQuantity(item.getReservedQuantity())
                    .subtract(normalizeQuantity(reservation.getReservedQuantity()));

            if (newReservedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                newReservedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
            }

            item.setReservedQuantity(newReservedQuantity);

            reservation.setStatus(InventoryReservationStatus.RELEASED);
            reservation.setReleasedAt(releasedAt);
        }

        inventoryItemRepository.saveAll(lockedItemsById.values());
        reservationRepository.saveAll(reservations);
        inventoryAvailabilityService.syncAvailabilityForInventoryItems(lockedItemsById.keySet(), true);
        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_released", orderItemId));
    }

    @Transactional
    public void consumeForOrderItem(@NonNull OrderItem orderItem) {
        Long orderItemId = orderItem.getId();

        List<OrderItemInventoryReservation> reservations = reservationRepository.findByOrderItemIdAndStatusForUpdate(
                orderItemId, InventoryReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            return;
        }

        Map<Long, InventoryItem> lockedItemsById = lockReservationInventoryItems(reservations);
        var consumedAt = AppTime.now();

        for (OrderItemInventoryReservation reservation : reservations) {
            InventoryItem item =
                    lockedItemsById.get(reservation.getInventoryItem().getId());

            BigDecimal consumedQuantity = normalizeQuantity(reservation.getReservedQuantity());

            BigDecimal quantityBefore = normalizeQuantity(item.getQuantityOnHand());
            BigDecimal quantityAfter = quantityBefore.subtract(consumedQuantity);

            if (quantityAfter.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
            }

            BigDecimal newReservedQuantity =
                    normalizeQuantity(item.getReservedQuantity()).subtract(consumedQuantity);

            if (newReservedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                newReservedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
            }

            item.setQuantityOnHand(quantityAfter);
            item.setReservedQuantity(newReservedQuantity);

            reservation.setStatus(InventoryReservationStatus.CONSUMED);
            reservation.setConsumedAt(consumedAt);

            stockMovementService.recordMovement(
                    item,
                    orderItem,
                    StockMovementType.CONSUME,
                    consumedQuantity,
                    quantityBefore,
                    quantityAfter,
                    "Consume inventory for order item #" + orderItemId);
        }

        inventoryItemRepository.saveAll(lockedItemsById.values());
        reservationRepository.saveAll(reservations);
        inventoryAvailabilityService.syncAvailabilityForInventoryItems(lockedItemsById.keySet(), false);
        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_consumed", orderItemId));
    }

    private List<ReservationDraft> buildReservationDrafts(OrderItem orderItem, BigDecimal orderItemQuantity) {
        Map<Long, ReservationDraft> drafts = new LinkedHashMap<>();
        List<MenuItemRequirement> menuItemRequirements = new ArrayList<>();

        if (orderItem.getMenuItem() != null && orderItem.getMenuItem().getId() != null) {
            menuItemRequirements.add(
                    new MenuItemRequirement(orderItem.getMenuItem().getId(), orderItemQuantity));
        }

        if (orderItem.getCombo() != null && orderItem.getCombo().getItems() != null) {
            for (var comboItem : orderItem.getCombo().getItems()) {
                if (comboItem.getMenuItem() == null) {
                    continue;
                }

                BigDecimal comboItemQuantity = quantityFrom(comboItem.getQuantity());
                BigDecimal multiplier = orderItemQuantity.multiply(comboItemQuantity);

                menuItemRequirements.add(
                        new MenuItemRequirement(comboItem.getMenuItem().getId(), multiplier));
            }
        }

        if (menuItemRequirements.isEmpty()) {
            return List.of();
        }

        List<Long> menuItemIds = menuItemRequirements.stream()
                .map(MenuItemRequirement::menuItemId)
                .distinct()
                .toList();
        Map<Long, List<RecipeItem>> recipeItemsByMenuItem = recipeItemRepository.findByMenuItemIds(menuItemIds).stream()
                .collect(Collectors.groupingBy(r -> r.getMenuItem().getId()));

        for (MenuItemRequirement requirement : menuItemRequirements) {
            mergeMenuItemRequirementsFast(
                    drafts, requirement.menuItemId(), requirement.multiplier(), recipeItemsByMenuItem);
        }

        return new ArrayList<>(drafts.values());
    }

    private void upsertReservation(OrderItem orderItem, OrderItemInventoryReservation newReservation) {
        OrderItemInventoryReservation existingReservation = findExistingReservedReservation(
                orderItem, newReservation.getInventoryItem().getId());

        if (existingReservation != null) {
            existingReservation.setReservedQuantity(normalizeQuantity(existingReservation.getReservedQuantity())
                    .add(normalizeQuantity(newReservation.getReservedQuantity())));

            if (existingReservation.getId() != null) {
                reservationRepository.save(existingReservation);
            }

            return;
        }

        reservationRepository.save(newReservation);
    }

    private OrderItemInventoryReservation findExistingReservedReservation(OrderItem orderItem, Long inventoryItemId) {
        if (orderItem.getId() != null) {
            return reservationRepository
                    .findByOrderItem_IdAndInventoryItem_IdAndStatus(
                            orderItem.getId(), inventoryItemId, InventoryReservationStatus.RESERVED)
                    .orElse(null);
        }

        return null;
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
                            oldValue.requiredQuantity()
                                    .add(newValue.requiredQuantity())
                                    .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP)));
        }
    }

    private Map<Long, InventoryItem> lockReservationInventoryItems(List<OrderItemInventoryReservation> reservations) {
        List<Long> sortedInventoryItemIds = reservations.stream()
                .map(OrderItemInventoryReservation::getInventoryItem)
                .filter(Objects::nonNull)
                .map(InventoryItem::getId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        return getInventoryItemsForUpdate(sortedInventoryItemIds);
    }

    private Map<Long, InventoryItem> getInventoryItemsForUpdate(List<Long> sortedInventoryItemIds) {
        if (sortedInventoryItemIds.isEmpty()) {
            return Map.of();
        }

        List<InventoryItem> lockedItems = inventoryItemRepository.findAllByIdInForUpdate(sortedInventoryItemIds);
        Map<Long, InventoryItem> lockedItemsById = lockedItems.stream()
                .collect(Collectors.toMap(
                        InventoryItem::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        for (Long inventoryItemId : sortedInventoryItemIds) {
            if (!lockedItemsById.containsKey(inventoryItemId)) {
                throw new BusinessException(ErrorCode.INVENTORY_ITEM_NOT_FOUND);
            }
        }

        return lockedItemsById;
    }

    private BigDecimal quantityFrom(Number value) {
        return InventoryQuantitySupport.quantityFrom(value);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        return InventoryQuantitySupport.normalizeQuantity(value);
    }

    private record MenuItemRequirement(Long menuItemId, BigDecimal multiplier) {}

    private record ReservationDraft(Long inventoryItemId, BigDecimal requiredQuantity) {}
}
