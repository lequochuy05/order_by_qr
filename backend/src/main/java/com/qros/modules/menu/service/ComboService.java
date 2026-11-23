package com.qros.modules.menu.service;

import com.qros.modules.inventory.service.InventoryAvailabilityService;
import com.qros.modules.menu.dto.publicmenu.PublicComboItem;
import com.qros.modules.menu.dto.request.ComboItemRequest;
import com.qros.modules.menu.dto.request.ComboRequest;
import com.qros.modules.menu.dto.response.ComboResponse;
import com.qros.modules.menu.mapper.ComboMapper;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboItemRepository;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboService {

    private final ComboRepository comboRepo;
    private final ComboItemRepository comboItemRepo;
    private final MenuItemRepository menuItemRepo;

    private final ComboMapper comboMapper;
    private final PublicMenuMapper publicMenuMapper;

    private final ApplicationEventPublisher eventPublisher;
    private final InventoryAvailabilityService inventoryAvailabilityService;

    @Cacheable(value = CacheNames.COMBOS, key = "'all_active'")
    public List<ComboResponse> getAllActive() {
        List<Combo> combos = comboRepo.findAllActiveWithItems();
        List<Long> comboIds = combos.stream().map(Combo::getId).toList();
        Map<Long, Boolean> availability = inventoryAvailabilityService.getComboAvailabilityByIds(comboIds);
        return combos.stream()
                .filter(combo -> availability.getOrDefault(combo.getId(), true))
                .map(comboMapper::toResponse)
                .toList();
    }

    @Cacheable(value = CacheNames.COMBOS, key = "'public_all_active'")
    public List<PublicComboItem> getPublicActive() {
        return getPublicActive(false);
    }

    @Cacheable(value = CacheNames.COMBOS, key = "'public_all_active:' + #includeUnavailable")
    public List<PublicComboItem> getPublicActive(boolean includeUnavailable) {
        List<Combo> combos =
                includeUnavailable ? comboRepo.findAllPublicWithItems() : comboRepo.findAllActiveWithItems();
        return combos.stream().map(publicMenuMapper::toComboItem).toList();
    }

    public Page<ComboResponse> searchSummaries(String keyword, Boolean active, @NonNull Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Combo> page = comboRepo.searchSummaries(normalizedKeyword, active, pageable);
        List<Long> comboIds = page.getContent().stream().map(Combo::getId).toList();
        Map<Long, Boolean> availability = inventoryAvailabilityService.getComboAvailabilityByIds(comboIds);

        return page.map(combo -> {
            ComboResponse response = comboMapper.toSummaryResponse(combo);
            boolean effectiveAvailable =
                    Boolean.TRUE.equals(combo.getAvailable()) && availability.getOrDefault(combo.getId(), true);
            return applyAvailability(response, effectiveAvailable);
        });
    }

    @Cacheable(value = CacheNames.COMBOS, key = "'item_' + #id", unless = "#result == null")
    public ComboResponse getById(@NonNull Long id) {
        Combo combo = getEntityByIdWithItems(id);
        boolean canPrepare = inventoryAvailabilityService
                .getComboAvailabilityByIds(List.of(id))
                .getOrDefault(id, true);
        boolean effectiveAvailable = Boolean.TRUE.equals(combo.getAvailable()) && canPrepare;
        return applyAvailability(comboMapper.toResponse(combo), effectiveAvailable);
    }

    @Transactional
    public ComboResponse create(@NonNull ComboRequest req) {
        String name = normalizeRequired(req.name(), "Combo name cannot be empty");

        if (comboRepo.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.COMBO_NAME_EXISTS);
        }

        validateItems(req.items());

        Combo combo = Combo.builder()
                .name(name)
                .description(normalizeBlank(req.description()))
                .price(req.price())
                .active(req.active() != null ? req.active() : true)
                .available(req.available() != null ? req.available() : true)
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .items(new LinkedHashSet<>())
                .build();

        Map<Long, MenuItem> menuItemMap = loadMenuItemMap(req.items());

        for (ComboItemRequest itemReq : req.items()) {
            MenuItem menuItem = menuItemMap.get(itemReq.menuItemId());

            if (menuItem == null) {
                throw new BusinessException(
                        ErrorCode.MENU_ITEM_NOT_FOUND, "Menu item not found: " + itemReq.menuItemId());
            }

            combo.getItems()
                    .add(ComboItem.builder()
                            .combo(combo)
                            .menuItem(menuItem)
                            .quantity(itemReq.quantity() != null ? itemReq.quantity() : 1)
                            .build());
        }

        boolean hasStock = inventoryAvailabilityService.hasSufficientInventoryForCombo(combo);
        boolean allItemsAvailable = inventoryAvailabilityService.areAllComboItemsOrderable(combo);
        if (!allItemsAvailable || !hasStock) {
            combo.setAvailable(false);
        }

        Combo saved = saveCombo(combo);
        eventPublisher.publishEvent(new ComboChangeEvent("created", saved.getId()));

        boolean effectiveAvailable = Boolean.TRUE.equals(saved.getAvailable()) && allItemsAvailable && hasStock;
        return applyAvailability(comboMapper.toResponse(saved), effectiveAvailable);
    }

    @Transactional
    public ComboResponse update(@NonNull Long id, @NonNull ComboRequest req) {
        Combo combo = getEntityByIdWithItems(id);
        assertVersionMatches(combo.getVersion(), req.version());

        String name = normalizeRequired(req.name(), "Combo name cannot be empty");

        if (!combo.getName().equalsIgnoreCase(name) && comboRepo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.COMBO_NAME_EXISTS);
        }

        validateItems(req.items());

        combo.setName(name);
        combo.setDescription(normalizeBlank(req.description()));
        combo.setPrice(req.price());

        if (req.active() != null) {
            combo.setActive(req.active());
        }

        if (req.available() != null) {
            combo.setAvailable(req.available());
        }

        if (req.displayOrder() != null) {
            combo.setDisplayOrder(req.displayOrder());
        }

        syncComboItems(combo, req.items());

        boolean hasStock = inventoryAvailabilityService.hasSufficientInventoryForCombo(combo);
        boolean allItemsAvailable = inventoryAvailabilityService.areAllComboItemsOrderable(combo);
        if (Boolean.TRUE.equals(req.available()) && !hasStock) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
        }
        if (!allItemsAvailable || !hasStock) {
            combo.setAvailable(false);
        }

        Combo saved = saveCombo(combo);
        eventPublisher.publishEvent(new ComboChangeEvent("updated", saved.getId()));

        boolean effectiveAvailable = Boolean.TRUE.equals(saved.getAvailable()) && allItemsAvailable && hasStock;
        return applyAvailability(comboMapper.toResponse(saved), effectiveAvailable);
    }

    @Transactional
    public void delete(@NonNull Long id, @NonNull Long version) {
        Combo combo = getEntityByIdWithItems(id);
        // L-fix: assert version to prevent concurrent delete race condition.
        assertVersionMatches(combo.getVersion(), version);

        // Redundant-bulk-delete fix: removed comboItemRepo.softDeleteByComboId(id).
        // The CascadeType.ALL + orphanRemoval + @SQLDelete("UPDATE combo_items SET is_deleted = true WHERE id = ?")
        // on ComboItem already handles soft-deletion correctly within the same transaction.
        // The extra JPQL bulk UPDATE bypassed the L1 cache and triggered a second soft-delete per row.
        comboRepo.delete(combo);

        eventPublisher.publishEvent(new ComboChangeEvent("deleted", id));
    }

    @Transactional
    public ComboResponse toggleActive(@NonNull Long id, @NonNull Long expectedVersion) {
        Combo combo = getEntityByIdWithItems(id);
        assertVersionMatches(combo.getVersion(), expectedVersion);

        combo.setActive(!Boolean.TRUE.equals(combo.getActive()));

        Combo saved = comboRepo.save(combo);
        eventPublisher.publishEvent(new ComboChangeEvent("status_updated", saved.getId()));

        return comboMapper.toResponse(saved);
    }

    @Transactional
    public ComboResponse toggleAvailable(@NonNull Long id, Long expectedVersion) {
        Combo combo = getEntityByIdWithItems(id);
        if (expectedVersion != null) {
            assertVersionMatches(combo.getVersion(), expectedVersion);
        }

        boolean hasStock = inventoryAvailabilityService.hasSufficientInventoryForCombo(combo);
        boolean allItemsAvailable = inventoryAvailabilityService.areAllComboItemsOrderable(combo);
        boolean currentlyAvailable = Boolean.TRUE.equals(combo.getAvailable()) && allItemsAvailable && hasStock;
        boolean nextAvailable = !currentlyAvailable;

        if (nextAvailable) {
            validateAllComboItemsActive(combo);
            if (!hasStock) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
            }
            enableUnavailableConstituentItems(combo);
        }

        combo.setAvailable(nextAvailable);

        Combo saved = comboRepo.saveAndFlush(combo);
        eventPublisher.publishEvent(new ComboChangeEvent("availability_updated", saved.getId()));

        boolean effectiveAvailable = Boolean.TRUE.equals(saved.getAvailable())
                && inventoryAvailabilityService.areAllComboItemsOrderable(saved)
                && hasStock;
        return applyAvailability(comboMapper.toResponse(saved), effectiveAvailable);
    }

    private Combo getEntityByIdWithItems(Long id) {
        return comboRepo.findByIdWithItems(id).orElseThrow(() -> new BusinessException(ErrorCode.COMBO_NOT_FOUND));
    }

    private void syncComboItems(Combo combo, List<ComboItemRequest> incomingItems) {
        if (incomingItems == null || incomingItems.isEmpty()) {
            combo.getItems().clear();
            return;
        }

        Map<Long, ComboItem> existingMap = combo.getItems().stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ComboItem::getId, item -> item));

        Set<Long> incomingExistingIds = incomingItems.stream()
                .map(ComboItemRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        combo.getItems().removeIf(item -> item.getId() != null && !incomingExistingIds.contains(item.getId()));

        Set<Long> menuItemIdsToLoad = incomingItems.stream()
                .filter(itemReq -> itemReq.id() == null)
                .map(ComboItemRequest::menuItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, MenuItem> menuItemMap = menuItemIdsToLoad.isEmpty()
                ? Collections.emptyMap()
                : menuItemRepo.findAllById(menuItemIdsToLoad).stream()
                        .collect(Collectors.toMap(MenuItem::getId, item -> item));

        for (ComboItemRequest itemReq : incomingItems) {
            Integer quantity = itemReq.quantity() != null ? itemReq.quantity() : 1;

            if (itemReq.id() != null) {
                ComboItem existing = existingMap.get(itemReq.id());

                if (existing == null) {
                    throw new BusinessException(
                            ErrorCode.BUSINESS_ERROR, "Combo item not found in this combo: " + itemReq.id());
                }

                Long existingMenuItemId =
                        existing.getMenuItem() != null ? existing.getMenuItem().getId() : null;
                if (!Objects.equals(existingMenuItemId, itemReq.menuItemId())) {
                    throw new BusinessException(
                            ErrorCode.BUSINESS_ERROR,
                            "Combo item menu item cannot be changed. Remove it and add a new item instead.");
                }

                existing.setQuantity(quantity);
                continue;
            }

            MenuItem menuItem = menuItemMap.get(itemReq.menuItemId());

            if (menuItem == null) {
                throw new BusinessException(
                        ErrorCode.MENU_ITEM_NOT_FOUND, "Menu item not found: " + itemReq.menuItemId());
            }

            combo.getItems()
                    .add(ComboItem.builder()
                            .combo(combo)
                            .menuItem(menuItem)
                            .quantity(quantity)
                            .build());
        }
    }

    private Map<Long, MenuItem> loadMenuItemMap(List<ComboItemRequest> items) {
        Set<Long> menuItemIds = items.stream()
                .map(ComboItemRequest::menuItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (menuItemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return menuItemRepo.findAllById(menuItemIds).stream().collect(Collectors.toMap(MenuItem::getId, item -> item));
    }

    private void validateItems(List<ComboItemRequest> items) {
        if (items == null || items.size() < 2) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Combo must contain at least two items");
        }

        Set<Long> menuItemIds = new HashSet<>();

        for (ComboItemRequest item : items) {
            if (item.menuItemId() == null && item.id() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Menu item ID is required");
            }

            if (item.quantity() != null && item.quantity() < 1) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Combo item quantity must be at least 1");
            }

            if (item.menuItemId() != null && !menuItemIds.add(item.menuItemId())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR, "Duplicate menu item in combo: " + item.menuItemId());
            }
        }
    }

    private void assertVersionMatches(Long currentVersion, Long expectedVersion) {
        if (expectedVersion == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST, "Combo version is required. Please refresh and try again.");
        }

        if (!Objects.equals(currentVersion, expectedVersion)) {
            throw new BusinessException(
                    ErrorCode.CONCURRENT_MODIFICATION,
                    "This combo was changed by another request. Please refresh and try again.");
        }
    }

    private Combo saveCombo(Combo combo) {
        try {
            return comboRepo.saveAndFlush(combo);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.COMBO_NAME_EXISTS, "Combo name already exists", exception);
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }

        return value.trim();
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateAllComboItemsActive(Combo combo) {
        if (combo.getItems() == null) {
            return;
        }
        boolean anyInactive = combo.getItems().stream()
                .filter(item -> item != null && !item.isDeleted())
                .anyMatch(item -> item.getMenuItem() == null
                        || item.getMenuItem().isDeleted()
                        || !Boolean.TRUE.equals(item.getMenuItem().getActive()));
        if (anyInactive) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "Cannot enable combo because one or more constituent menu items are inactive");
        }
    }

    private void enableUnavailableConstituentItems(Combo combo) {
        if (combo.getItems() == null) {
            return;
        }
        List<MenuItem> reEnabledItems = new ArrayList<>();
        for (ComboItem comboItem : combo.getItems()) {
            if (comboItem == null || comboItem.isDeleted() || comboItem.getMenuItem() == null) {
                continue;
            }
            MenuItem menuItem = comboItem.getMenuItem();
            if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
                menuItem.setAvailable(true);
                MenuItem savedMenuItem = menuItemRepo.saveAndFlush(menuItem);
                reEnabledItems.add(savedMenuItem);
                eventPublisher.publishEvent(new MenuChangeEvent("availability_updated", savedMenuItem.getId()));
            }
        }
        for (MenuItem reEnabled : reEnabledItems) {
            inventoryAvailabilityService.syncCombosForMenuItem(reEnabled);
        }
    }

    private ComboResponse applyAvailability(ComboResponse response, boolean effectiveAvailable) {
        if (response == null || Objects.equals(response.available(), effectiveAvailable)) {
            return response;
        }
        return comboMapper.withAvailability(response, effectiveAvailable);
    }
}
