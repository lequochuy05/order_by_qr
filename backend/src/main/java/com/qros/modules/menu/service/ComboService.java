package com.qros.modules.menu.service;

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
import org.springframework.context.ApplicationEventPublisher;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

    @Cacheable(value = CacheNames.COMBOS, key = "'all_active'")
    public List<ComboResponse> getAllActive() {
        return comboRepo.findAllActiveWithItems().stream()
                .map(comboMapper::toResponse)
                .toList();
    }

    @Cacheable(value = CacheNames.COMBOS, key = "'public_all_active'")
    public List<PublicComboItem> getPublicActive() {
        return comboRepo.findAllActiveWithItems().stream()
                .map(publicMenuMapper::toComboItem)
                .toList();
    }

    @Cacheable(value = CacheNames.COMBOS, key = "'all'")
    public List<ComboResponse> getAll() {
        return comboRepo.searchManagementSummaries(null, null, Pageable.unpaged()).stream()
                .map(comboMapper::toSummaryResponse)
                .toList();
    }

    public Page<ComboResponse> searchManagementSummary(String keyword, Boolean active, @NonNull Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return comboRepo.searchManagementSummaries(normalizedKeyword, active, pageable)
                .map(comboMapper::toSummaryResponse);
    }

    @Cacheable(value = CacheNames.COMBOS, key = "'item_' + #id", unless = "#result == null")
    public ComboResponse getById(@NonNull Long id) {
        return comboMapper.toResponse(getEntityByIdWithItems(id));
    }

    @Transactional
    @CacheEvict(
            value = { CacheNames.COMBOS, CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS },
            allEntries = true
    )
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
                        ErrorCode.MENU_ITEM_NOT_FOUND,
                        "Menu item not found: " + itemReq.menuItemId()
                );
            }

            combo.getItems().add(ComboItem.builder()
                    .combo(combo)
                    .menuItem(menuItem)
                    .quantity(itemReq.quantity() != null ? itemReq.quantity() : 1)
                    .build());
        }

        Combo saved = comboRepo.save(combo);
        eventPublisher.publishEvent(new ComboChangeEvent("created", saved.getId()));

        return comboMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(
            value = { CacheNames.COMBOS, CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS },
            allEntries = true
    )
    public ComboResponse update(@NonNull Long id, @NonNull ComboRequest req) {
        Combo combo = getEntityByIdWithItems(id);

        String name = normalizeRequired(req.name(), "Combo name cannot be empty");

        if (!combo.getName().equalsIgnoreCase(name)
                && comboRepo.existsByNameIgnoreCaseAndIdNot(name, id)) {
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

        Combo saved = comboRepo.save(combo);
        eventPublisher.publishEvent(new ComboChangeEvent("updated", saved.getId()));

        return comboMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(
            value = { CacheNames.COMBOS, CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS },
            allEntries = true
    )
    public void delete(@NonNull Long id) {
        Combo combo = getEntityByIdWithItems(id);

        comboItemRepo.softDeleteByComboId(id);
        comboRepo.delete(combo);

        eventPublisher.publishEvent(new ComboChangeEvent("deleted", id));
    }

    @Transactional
    @CacheEvict(
            value = { CacheNames.COMBOS, CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS },
            allEntries = true
    )
    public ComboResponse toggleActive(@NonNull Long id) {
        Combo combo = getEntityByIdWithItems(id);

        combo.setActive(!Boolean.TRUE.equals(combo.getActive()));

        Combo saved = comboRepo.save(combo);
        eventPublisher.publishEvent(new ComboChangeEvent("status_updated", saved.getId()));

        return comboMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(
            value = { CacheNames.COMBOS, CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS },
            allEntries = true
    )
    public ComboResponse toggleAvailable(@NonNull Long id) {
        Combo combo = getEntityByIdWithItems(id);

        combo.setAvailable(!Boolean.TRUE.equals(combo.getAvailable()));

        Combo saved = comboRepo.save(combo);
        eventPublisher.publishEvent(new ComboChangeEvent("availability_updated", saved.getId()));

        return comboMapper.toResponse(saved);
    }

    private Combo getEntityByIdWithItems(Long id) {
        return comboRepo.findByIdWithItems(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMBO_NOT_FOUND));
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

        combo.getItems().removeIf(item ->
                item.getId() != null && !incomingExistingIds.contains(item.getId())
        );

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
                            ErrorCode.BUSINESS_ERROR,
                            "Combo item not found in this combo: " + itemReq.id()
                    );
                }

                existing.setQuantity(quantity);
                continue;
            }

            MenuItem menuItem = menuItemMap.get(itemReq.menuItemId());

            if (menuItem == null) {
                throw new BusinessException(
                        ErrorCode.MENU_ITEM_NOT_FOUND,
                        "Menu item not found: " + itemReq.menuItemId()
                );
            }

            combo.getItems().add(ComboItem.builder()
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

        return menuItemRepo.findAllById(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));
    }

    private void validateItems(List<ComboItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "Combo must contain at least one item"
            );
        }

        Set<Long> menuItemIds = new HashSet<>();

        for (ComboItemRequest item : items) {
            if (item.menuItemId() == null && item.id() == null) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "Menu item ID is required"
                );
            }

            if (item.quantity() != null && item.quantity() < 1) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "Combo item quantity must be at least 1"
                );
            }

            if (item.menuItemId() != null && !menuItemIds.add(item.menuItemId())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "Duplicate menu item in combo: " + item.menuItemId()
                );
            }
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
}
