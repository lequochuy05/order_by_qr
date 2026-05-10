package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.ComboItemRequest;
import com.sacmauquan.qrordering.dto.ComboRequest;
import com.sacmauquan.qrordering.model.Combo;
import com.sacmauquan.qrordering.model.ComboItem;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.ComboItemRepository;
import com.sacmauquan.qrordering.repository.ComboRepository;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ComboService - Manages combos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepo;
    private final ComboItemRepository comboItemRepo;
    private final MenuItemRepository menuItemRepo;
    private final NotificationService notificationService;

    /**
     * Get all active combos with items
     */
    @Cacheable(value = "combos", key = "'all_active'")
    public List<Combo> getAllActive() {
        return comboRepo.findAllActiveWithItems();
    }

    /**
     * Get all combos
     */
    public List<Combo> getAll() {
        return comboRepo.findAll();
    }

    /**
     * Get combo by ID
     */
    public Combo getById(@NonNull Long id) {
        return comboRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo not found"));
    }

    /**
     * Create a new combo
     */
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public Combo create(ComboRequest req) {
        if (comboRepo.existsByNameIncludingDeleted(req.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo name already exists");
        }

        Combo combo = Combo.builder()
                .name(req.getName())
                .price(req.getPrice())
                .active(req.getActive() != null ? req.getActive() : true)
                .items(new LinkedHashSet<>())
                .build();

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            Set<Long> menuIds = req.getItems().stream()
                    .map(ComboItemRequest::getMenuItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, MenuItem> menuMap = menuItemRepo.findAllById(Objects.requireNonNull(menuIds)).stream()
                    .collect(Collectors.toMap(MenuItem::getId, m -> m));

            for (ComboItemRequest itemReq : req.getItems()) {
                MenuItem menuItem = menuMap.get(itemReq.getMenuItemId());
                if (menuItem == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Menu item not found: " + itemReq.getMenuItemId());
                }

                combo.getItems().add(ComboItem.builder()
                        .combo(combo)
                        .menuItem(menuItem)
                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                        .build());
            }
        }

        Combo saved = comboRepo.save(Objects.requireNonNull(combo));
        notificationService.notifyComboChange("created", saved.getId());
        return saved;
    }

    /**
     * Update a combo
     */
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public Combo update(@NonNull Long id, ComboRequest req) {
        Combo combo = getById(id);
        if (!combo.getName().equalsIgnoreCase(req.getName()) && comboRepo.existsByNameIncludingDeleted(req.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo name already exists");
        }

        combo.setName(req.getName());
        combo.setPrice(req.getPrice());
        combo.setActive(req.getActive() != null ? req.getActive() : combo.getActive());

        syncComboItems(combo, req.getItems());

        Combo saved = comboRepo.save(Objects.requireNonNull(combo));
        notificationService.notifyComboChange("updated", saved.getId());
        return saved;
    }

    /**
     * Sync combo items (Add new, update existing, remove removed)
     */
    private void syncComboItems(Combo combo, List<ComboItemRequest> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            combo.getItems().clear();
            return;
        }

        // Map existing items by their ID
        Map<Long, ComboItem> existingMap = combo.getItems().stream()
                .collect(Collectors.toMap(ComboItem::getId, item -> item));

        // Get all incoming item IDs
        Set<Long> incomingIds = incoming.stream()
                .map(ComboItemRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Remove items that are not in the incoming list
        combo.getItems().removeIf(item -> !incomingIds.contains(item.getId()));

        // Get new items
        List<ComboItemRequest> newItemsReq = incoming.stream()
                .filter(req -> req.getId() == null)
                .toList();

        // Get menu items for new items
        Set<Long> newMenuIds = newItemsReq.stream()
                .map(ComboItemRequest::getMenuItemId)
                .collect(Collectors.toSet());

        Map<Long, MenuItem> menuMap = newMenuIds.isEmpty() ? Collections.emptyMap()
                : menuItemRepo.findAllById(newMenuIds).stream()
                        .collect(Collectors.toMap(MenuItem::getId, m -> m));

        // Update existing items and add new items
        for (ComboItemRequest itemReq : incoming) {
            if (itemReq.getId() != null && existingMap.containsKey(itemReq.getId())) {
                ComboItem existing = existingMap.get(itemReq.getId());
                existing.setQuantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1);
            } else if (itemReq.getId() == null) {
                MenuItem menuItem = menuMap.get(itemReq.getMenuItemId());
                if (menuItem == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Menu item not found: " + itemReq.getMenuItemId());
                }
                combo.getItems().add(ComboItem.builder()
                        .combo(combo)
                        .menuItem(menuItem)
                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                        .build());
            }
        }
    }

    /**
     * Delete a combo
     */
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public void delete(@NonNull Long id) {
        Combo combo = getById(id);

        comboItemRepo.softDeleteByComboId(id);
        comboRepo.delete(Objects.requireNonNull(combo));

        notificationService.notifyComboChange("deleted", id);
    }

    /**
     * Toggle active status of a combo
     */
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public Combo toggleActive(@NonNull Long id) {
        Combo combo = getById(id);
        combo.setActive(!Boolean.TRUE.equals(combo.getActive()));

        notificationService.notifyComboChange("status_updated", id);
        return comboRepo.save(Objects.requireNonNull(combo));
    }
}
