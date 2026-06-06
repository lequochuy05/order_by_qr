package com.qros.modules.menu.service.impl;

import com.qros.infrastructure.storage.ImageManagerService;
import com.qros.modules.menu.dto.MenuItemRequest;
import com.qros.modules.menu.dto.MenuItemResponse;
import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.transaction.TransactionSideEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MenuItemServiceImpl - Menu item management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;
    private final ImageManagerService imageManager;
    private final TransactionSideEffectService sideEffects;

    /**
     * Get all menu items
     */
    @Override
    @Cacheable(value = "menu", key = "'all'")
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "menu", key = "'public_all'")
    public List<PublicMenuResponse.MenuItemItem> getPublicMenuItems() {
        return menuItemRepository.findAllByActiveTrue().stream()
                .map(PublicMenuResponse::fromMenuItem)
                .toList();
    }

    /**
     * Get menu items by category
     */
    @Override
    @Cacheable(value = "menu", key = "'category_' + #categoryId")
    public List<MenuItemResponse> getItemsByCategory(@NonNull Integer categoryId) {
        return menuItemRepository.findByCategoryIdAndActiveTrue(Objects.requireNonNull(categoryId)).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "menu", key = "'public_category_' + #categoryId")
    public List<PublicMenuResponse.MenuItemItem> getPublicItemsByCategory(@NonNull Integer categoryId) {
        return menuItemRepository.findByCategoryIdAndActiveTrue(Objects.requireNonNull(categoryId)).stream()
                .map(PublicMenuResponse::fromMenuItem)
                .toList();
    }

    /**
     * Get menu item by id
     */
    @Override
    @Cacheable(value = "menu", key = "'item_' + #id", unless = "#result == null")
    public MenuItemResponse getItemById(@NonNull Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found item"));

        return convertToResponse(item);
    }

    /**
     * Create menu item
     */
    @Override
    @Transactional
    @CacheEvict(value = { "menu", "combos", "recommendations", "popularItems" }, allEntries = true)
    public MenuItemResponse createItem(@NonNull MenuItemRequest req) {
        if (menuItemRepository.existsByNameIgnoreCase(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item name already exists");
        }

        Category category = categoryRepository.findById(Objects.requireNonNull(req.getCategoryId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        MenuItem.MenuItemBuilder<?, ?> itemBuilder = MenuItem.builder()
                .name(req.getName())
                .price(req.getPrice())
                .active(req.getActive() != null ? req.getActive() : true)
                .category(category);

        if (req.getImg() != null && !req.getImg().isBlank()) {
            itemBuilder.img(req.getImg());
        }

        MenuItem item = itemBuilder.build();

        if (req.getItemOptions() != null) {
            req.getItemOptions().forEach(optReq -> {
                ItemOption opt = buildItemOption(optReq, item);
                item.getItemOptions().add(opt);
            });
        }

        MenuItem saved = menuItemRepository.save(Objects.requireNonNull(item));
        notificationService.notifyMenuChange("create", saved.getId());
        return convertToResponse(saved);
    }

    /**
     * Update menu item
     */
    @Override
    @Transactional
    @CacheEvict(value = { "menu", "combos", "recommendations", "popularItems" }, allEntries = true)
    public MenuItemResponse updateItem(@NonNull Long id, @NonNull MenuItemRequest req) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found item"));

        if (menuItemRepository.existsByNameIgnoreCaseAndIdNot(req.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item name already exists");
        }

        Category category = categoryRepository.findById(Objects.requireNonNull(req.getCategoryId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        item.setName(req.getName());
        item.setPrice(req.getPrice());
        item.setCategory(category);
        item.setActive(req.getActive() != null ? req.getActive() : item.getActive());
        if (req.getImg() != null)
            item.setImg(req.getImg());

        syncOptions(item, req.getItemOptions());

        MenuItem saved = menuItemRepository.save(item);
        notificationService.notifyMenuChange("update", saved.getId());
        return convertToResponse(saved);
    }

    /**
     * Delete menu item
     */
    @Override
    @Transactional
    @CacheEvict(value = { "menu", "combos", "recommendations", "popularItems" }, allEntries = true)
    public void deleteItem(@NonNull Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found item"));

        String oldImg = item.getImg();
        menuItemRepository.delete(item);
        sideEffects.afterCommit(() -> imageManager.delete(oldImg),
                "delete menu item image after item delete " + id);
        notificationService.notifyMenuChange("delete", id);
    }

    /**
     * Upload image
     */
    @Override
    @Transactional
    @CacheEvict(value = { "menu", "combos", "recommendations", "popularItems" }, allEntries = true)
    public Map<String, Object> uploadImage(@NonNull Long id, @NonNull MultipartFile file) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found item"));

        try {
            String oldUrl = item.getImg();
            String newUrl = imageManager.upload(file, "order_by_qr/menu_items");
            sideEffects.afterRollback(() -> imageManager.delete(newUrl),
                    "delete rolled back menu item image " + id);
            if (oldUrl != null) {
                sideEffects.afterCommit(() -> imageManager.delete(oldUrl),
                        "delete replaced menu item image " + id);
            }
            item.setImg(newUrl);
            menuItemRepository.save(item);
            notificationService.notifyMenuChange("upload_image", id);
            return Map.of("img", newUrl);
        } catch (IOException e) {
            log.error("Error uploading image for item {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error system when upload image");
        }
    }

    /**
     * Sync options
     */
    private void syncOptions(MenuItem item, List<MenuItemRequest.ItemOptionRequest> incoming) {
        if (incoming == null) {
            item.getItemOptions().clear();
            return;
        }

        Map<Long, ItemOption> existingMap = item.getItemOptions().stream()
                .collect(Collectors.toMap(ItemOption::getId, o -> o));

        Set<Long> incomingIds = incoming.stream()
                .map(MenuItemRequest.ItemOptionRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Delete option if not in incoming list
        item.getItemOptions().removeIf(o -> !incomingIds.contains(o.getId()));

        for (MenuItemRequest.ItemOptionRequest optReq : incoming) {
            if (optReq.getId() != null && existingMap.containsKey(optReq.getId())) {
                // Update existing
                ItemOption existing = existingMap.get(optReq.getId());
                existing.setName(optReq.getName());
                existing.setRequired(optReq.isRequired());
                existing.setMaxSelection(optReq.getMaxSelection());
                syncValues(existing, optReq.getOptionValues());
            } else {
                // Add new
                item.getItemOptions().add(buildItemOption(optReq, item));
            }
        }
    }

    /**
     * Sync values
     */
    private void syncValues(ItemOption opt, List<MenuItemRequest.ItemOptionValueRequest> incoming) {
        if (incoming == null) {
            opt.getOptionValues().clear();
            return;
        }

        Map<Long, ItemOptionValue> existingMap = opt.getOptionValues().stream()
                .collect(Collectors.toMap(ItemOptionValue::getId, v -> v));

        Set<Long> incomingIds = incoming.stream()
                .map(MenuItemRequest.ItemOptionValueRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        opt.getOptionValues().removeIf(v -> !incomingIds.contains(v.getId()));

        for (MenuItemRequest.ItemOptionValueRequest valReq : incoming) {
            if (valReq.getId() != null && existingMap.containsKey(valReq.getId())) {
                ItemOptionValue existing = existingMap.get(valReq.getId());
                existing.setName(valReq.getName());
                existing.setExtraPrice(valReq.getExtraPrice());
            } else {
                opt.getOptionValues().add(ItemOptionValue.builder()
                        .name(valReq.getName())
                        .extraPrice(valReq.getExtraPrice())
                        .itemOption(opt)
                        .build());
            }
        }
    }

    /**
     * Build item option
     */
    private ItemOption buildItemOption(MenuItemRequest.ItemOptionRequest optReq, MenuItem item) {
        ItemOption opt = ItemOption.builder()
                .name(optReq.getName())
                .isRequired(optReq.isRequired())
                .maxSelection(optReq.getMaxSelection())
                .menuItem(item)
                .build();
        if (optReq.getOptionValues() != null) {
            optReq.getOptionValues().forEach(valReq -> {
                opt.getOptionValues().add(ItemOptionValue.builder()
                        .name(valReq.getName())
                        .extraPrice(valReq.getExtraPrice())
                        .itemOption(opt)
                        .build());
            });
        }
        return opt;
    }

    /**
     * Convert to response
     */
    private MenuItemResponse convertToResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getImg(),
                item.getPrice(),
                item.getActive(),
                new MenuItemResponse.CategorySummary(item.getCategory().getId(), item.getCategory().getName()),
                item.getItemOptions().stream().map(o -> new MenuItemResponse.ItemOptionResponse(
                        o.getId(),
                        o.getName(),
                        o.isRequired(),
                        o.getMaxSelection(),
                        o.getOptionValues().stream().map(v -> new MenuItemResponse.ItemOptionValueResponse(
                                v.getId(), v.getName(), v.getExtraPrice())).toList()))
                        .toList(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
