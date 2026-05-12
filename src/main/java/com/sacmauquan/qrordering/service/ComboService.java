package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.ComboItemRequest;
import com.sacmauquan.qrordering.dto.ComboRequest;
import com.sacmauquan.qrordering.dto.ComboResponse;
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
 * ComboService - Manages bundled menu items (combos).
 * Handles complex synchronization between combos and their constituent items.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboService {

        private final ComboRepository comboRepo;
        private final ComboItemRepository comboItemRepo;
        private final MenuItemRepository menuItemRepo;
        private final NotificationService notificationService;

        /**
         * Retrieves all active combos including their pre-fetched items.
         * 
         * @return List of active combos
         */
        @Cacheable(value = "combos", key = "'all_active'")
        public List<ComboResponse> getAllActive() {
                return comboRepo.findAllActiveWithItems().stream()
                                .map(this::convertToResponse)
                                .toList();
        }

        /**
         * Retrieves all combos in the system.
         * 
         * @return List of all combos
         */
        public List<ComboResponse> getAll() {
                return comboRepo.findAll().stream()
                                .map(this::convertToResponse)
                                .toList();
        }

        /**
         * Retrieves a single combo by its identifier.
         * 
         * @param id Combo ID
         * @return ComboResponse DTO
         * @throws ResponseStatusException if combo not found
         */
        public ComboResponse getById(@NonNull Long id) {
                Combo combo = comboRepo.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Combo not found"));
                return convertToResponse(combo);
        }

        /**
         * Creates a new combo and its associated items.
         * Validates that all menu items exist before persisting.
         * 
         * @param req Combo creation request
         * @return Created ComboResponse DTO
         */
        @Transactional
        @CacheEvict(value = "combos", allEntries = true)
        public ComboResponse create(ComboRequest req) {
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
                return convertToResponse(saved);
        }

        /**
         * Updates an existing combo and synchronizes its item list.
         * 
         * @param id  Combo ID
         * @param req Update request
         * @return Updated ComboResponse DTO
         */
        @Transactional
        @CacheEvict(value = "combos", allEntries = true)
        public ComboResponse update(@NonNull Long id, ComboRequest req) {
                Combo combo = comboRepo.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Combo not found"));

                if (!combo.getName().equalsIgnoreCase(req.getName())
                                && comboRepo.existsByNameIncludingDeleted(req.getName())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo name already exists");
                }

                combo.setName(req.getName());
                combo.setPrice(req.getPrice());
                combo.setActive(req.getActive() != null ? req.getActive() : combo.getActive());

                syncComboItems(combo, req.getItems());

                Combo saved = comboRepo.save(Objects.requireNonNull(combo));
                notificationService.notifyComboChange("updated", saved.getId());
                return convertToResponse(saved);
        }

        /**
         * Internal helper to synchronize the combo's item set with incoming data.
         * Performs differential updates (add/update/remove).
         */
        private void syncComboItems(Combo combo, List<ComboItemRequest> incoming) {
                if (incoming == null || incoming.isEmpty()) {
                        combo.getItems().clear();
                        return;
                }

                Map<Long, ComboItem> existingMap = combo.getItems().stream()
                                .collect(Collectors.toMap(ComboItem::getId, item -> item));

                Set<Long> incomingIds = incoming.stream()
                                .map(ComboItemRequest::getId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                combo.getItems().removeIf(item -> !incomingIds.contains(item.getId()));

                List<ComboItemRequest> newItemsReq = incoming.stream()
                                .filter(req -> req.getId() == null)
                                .toList();

                Set<Long> newMenuIds = newItemsReq.stream()
                                .map(ComboItemRequest::getMenuItemId)
                                .collect(Collectors.toSet());

                Map<Long, MenuItem> menuMap = newMenuIds.isEmpty() ? Collections.emptyMap()
                                : menuItemRepo.findAllById(newMenuIds).stream()
                                                .collect(Collectors.toMap(MenuItem::getId, m -> m));

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
         * Soft deletes a combo and all its associated items.
         * 
         * @param id Combo ID
         */
        @Transactional
        @CacheEvict(value = "combos", allEntries = true)
        public void delete(@NonNull Long id) {
                Combo combo = comboRepo.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Combo not found"));

                comboItemRepo.softDeleteByComboId(id);
                comboRepo.delete(Objects.requireNonNull(combo));

                notificationService.notifyComboChange("deleted", id);
        }

        /**
         * Toggles the active status of a combo.
         * 
         * @param id Combo ID
         * @return Updated ComboResponse DTO
         */
        @Transactional
        @CacheEvict(value = "combos", allEntries = true)
        public ComboResponse toggleActive(@NonNull Long id) {
                Combo combo = comboRepo.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Combo not found"));
                combo.setActive(!Boolean.TRUE.equals(combo.getActive()));

                notificationService.notifyComboChange("status_updated", id);
                Combo saved = comboRepo.save(Objects.requireNonNull(combo));
                return convertToResponse(saved);
        }

        /**
         * Mapping helper to convert Combo entity to ComboResponse DTO.
         */
        private ComboResponse convertToResponse(Combo combo) {
                return ComboResponse.builder()
                                .id(combo.getId())
                                .name(combo.getName())
                                .price(combo.getPrice())
                                .active(combo.getActive())
                                .items(combo.getItems().stream()
                                                .map(item -> ComboResponse.ComboItemResponse.builder()
                                                                .id(item.getId())
                                                                .menuItemId(item.getMenuItem().getId())
                                                                .menuItemName(item.getMenuItem().getName())
                                                                .menuItemImg(item.getMenuItem().getImg())
                                                                .quantity(item.getQuantity())
                                                                .build())
                                                .toList())
                                .build();
        }
}
