package com.qros.modules.menu.service;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.menu.dto.publicmenu.PublicMenuItem;
import com.qros.modules.menu.dto.request.MenuItemRequest;
import com.qros.modules.menu.dto.response.MenuItemImageResponse;
import com.qros.modules.menu.dto.response.MenuItemResponse;
import com.qros.modules.menu.mapper.MenuItemMapper;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.ComboItemRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionExecutor;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.validation.ImageFileValidator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuItemService {

    private static final String DEFAULT_MENU_ITEM_IMAGE = "default_menu_item.png";
    private static final String MENU_ITEM_IMAGE_FOLDER = "order_by_qr/menu_items";

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final ComboItemRepository comboItemRepository;

    private final MenuItemMapper menuItemMapper;
    private final PublicMenuMapper publicMenuMapper;

    private final StorageService storageService;
    private final TransactionSideEffectService sideEffects;
    private final ApplicationEventPublisher eventPublisher;
    private final MenuItemOptionService menuItemOptionService;
    private final ImageFileValidator imageFileValidator;
    private final MenuInventoryAvailabilityPort inventoryAvailabilityService;
    private final MenuCatalogOrderUsagePort orderUsagePort;
    private final TransactionExecutor transactionExecutor;

    public Page<MenuItemResponse> searchSummaries(String keyword, Long categoryId, @NonNull Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<MenuItem> page = menuItemRepository.searchSummaries(normalizedKeyword, categoryId, pageable);
        List<Long> itemIds = page.getContent().stream().map(MenuItem::getId).toList();
        Map<Long, Boolean> stockAvailability = inventoryAvailabilityService.getMenuItemAvailability(itemIds);

        return page.map(item -> {
            MenuItemResponse response = menuItemMapper.toSummaryResponse(item);
            boolean effectiveAvailable =
                    Boolean.TRUE.equals(item.getAvailable()) && stockAvailability.getOrDefault(item.getId(), true);
            return applyAvailability(response, effectiveAvailable);
        });
    }

    public MenuItemResponse getById(@NonNull Long id) {
        MenuItem item = getEntityById(id);
        boolean hasStock = inventoryAvailabilityService
                .getMenuItemAvailability(List.of(id))
                .getOrDefault(id, true);
        boolean effectiveAvailable = Boolean.TRUE.equals(item.getAvailable()) && hasStock;
        return applyAvailability(menuItemMapper.toResponse(item), effectiveAvailable);
    }

    @Cacheable(value = CacheNames.MENU, key = "'public_items'")
    public List<PublicMenuItem> getPublicMenuItems() {
        return getPublicMenuItems(false);
    }

    @Cacheable(value = CacheNames.MENU, key = "'public_items:' + #includeUnavailable")
    public List<PublicMenuItem> getPublicMenuItems(boolean includeUnavailable) {
        List<MenuItem> items = includeUnavailable
                ? menuItemRepository.findAllPublicItems()
                : menuItemRepository.findAllPublicAvailableItems();
        return items.stream().map(publicMenuMapper::toMenuItem).toList();
    }

    @Cacheable(value = CacheNames.MENU, key = "'public_category_' + #categoryId")
    public List<PublicMenuItem> getPublicItemsByCategory(@NonNull Long categoryId) {
        return getPublicItemsByCategory(categoryId, false);
    }

    @Cacheable(value = CacheNames.MENU, key = "'public_category:' + #categoryId + ':' + #includeUnavailable")
    public List<PublicMenuItem> getPublicItemsByCategory(@NonNull Long categoryId, boolean includeUnavailable) {
        List<MenuItem> items = includeUnavailable
                ? menuItemRepository.findPublicItemsByCategoryId(categoryId)
                : menuItemRepository.findPublicAvailableItemsByCategoryId(categoryId);
        return items.stream().map(publicMenuMapper::toMenuItem).toList();
    }

    @Transactional
    public MenuItemResponse create(@NonNull MenuItemRequest req) {
        String name = MenuItemOptionService.normalizeRequired(req.name(), "Menu item name cannot be empty");

        if (menuItemRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NAME_EXISTS);
        }

        Category category = categoryRepository
                .findById(req.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        menuItemOptionService.validateOptions(req.itemOptions());

        boolean initialAvailable = req.available() != null ? req.available() : true;
        MenuItem item = MenuItem.builder()
                .name(name)
                .description(normalizeBlank(req.description()))
                .img(normalizeBlankOrDefault(req.img(), DEFAULT_MENU_ITEM_IMAGE))
                .price(req.price())
                .active(req.active() != null ? req.active() : true)
                .available(initialAvailable)
                .manuallyPaused(!initialAvailable)
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .category(category)
                .build();

        if (req.itemOptions() != null) {
            req.itemOptions()
                    .forEach(optionReq ->
                            item.getItemOptions().add(menuItemOptionService.buildItemOption(optionReq, item)));
        }

        MenuItem saved = saveMenuItem(item);
        eventPublisher.publishEvent(new MenuChangeEvent("created", saved.getId()));

        return menuItemMapper.toResponse(saved);
    }

    @Transactional
    public MenuItemResponse update(@NonNull Long id, @NonNull MenuItemRequest req) {
        MenuItem item = getEntityById(id);
        assertVersionMatches(item.getVersion(), req.version());

        String name = MenuItemOptionService.normalizeRequired(req.name(), "Menu item name cannot be empty");

        if (!item.getName().equalsIgnoreCase(name) && menuItemRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NAME_EXISTS);
        }

        Category category = categoryRepository
                .findById(req.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        menuItemOptionService.validateOptions(req.itemOptions());

        item.setName(name);
        item.setDescription(normalizeBlank(req.description()));
        item.setPrice(req.price());
        item.setCategory(category);

        if (req.img() != null && !req.img().isBlank()) {
            item.setImg(req.img().trim());
        }

        if (req.active() != null) {
            item.setActive(req.active());
        }

        if (req.available() != null) {
            boolean hasStock = inventoryAvailabilityService
                    .getMenuItemAvailability(List.of(id))
                    .getOrDefault(id, true);
            if (Boolean.TRUE.equals(req.available()) && !hasStock) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
            }
            item.setAvailable(req.available());
            item.setManuallyPaused(!Boolean.TRUE.equals(req.available()));
        }

        if (req.displayOrder() != null) {
            item.setDisplayOrder(req.displayOrder());
        }

        menuItemOptionService.syncOptions(item, req.itemOptions());

        MenuItem saved = saveMenuItem(item);
        inventoryAvailabilityService.syncCombosForMenuItem(saved);
        eventPublisher.publishEvent(new MenuChangeEvent("updated", saved.getId()));

        boolean hasStock = inventoryAvailabilityService
                .getMenuItemAvailability(List.of(saved.getId()))
                .getOrDefault(saved.getId(), true);
        return applyAvailability(
                menuItemMapper.toResponse(saved), Boolean.TRUE.equals(saved.getAvailable()) && hasStock);
    }

    @Transactional
    public MenuItemResponse toggleAvailable(@NonNull Long id, Long expectedVersion) {
        MenuItem item = getEntityById(id);
        if (expectedVersion != null) {
            assertVersionMatches(item.getVersion(), expectedVersion);
        }

        boolean hasStock = inventoryAvailabilityService
                .getMenuItemAvailability(List.of(id))
                .getOrDefault(id, true);
        boolean currentlyAvailable = Boolean.TRUE.equals(item.getAvailable()) && hasStock;
        boolean nextAvailable = !currentlyAvailable;

        if (nextAvailable && !hasStock) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
        }

        item.setAvailable(nextAvailable);
        item.setManuallyPaused(!nextAvailable);

        MenuItem saved = saveMenuItem(item);
        inventoryAvailabilityService.syncCombosForMenuItem(saved);
        eventPublisher.publishEvent(new MenuChangeEvent("availability_updated", saved.getId()));

        return applyAvailability(
                menuItemMapper.toResponse(saved), Boolean.TRUE.equals(saved.getAvailable()) && hasStock);
    }

    @Transactional
    public void delete(@NonNull Long id, @NonNull Long version) {
        MenuItem item = getEntityById(id);
        // L-fix: assert version to prevent concurrent delete race condition.
        assertVersionMatches(item.getVersion(), version);

        // Orphaned-reference fix: block deletion if the item is in ANY non-deleted combo
        // (active OR inactive). The old check only blocked active combos — allowing deletion
        // while in an inactive combo, which creates null references when that combo is re-activated.
        if (comboItemRepository.existsInAnyNonDeletedCombo(id)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "Cannot delete menu item that is part of a combo. Please remove it from all combos first.");
        }

        if (orderUsagePort.isMenuItemUsedInActiveOrders(id)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Cannot delete menu item that is referenced by active orders. Complete or cancel those orders first.");
        }

        String oldImg = item.getImg();

        menuItemRepository.delete(item);

        if (isCustomImage(oldImg)) {
            sideEffects.afterCommit(
                    () -> storageService.delete(oldImg), "delete menu item image after item delete " + id);
        }

        eventPublisher.publishEvent(new MenuItemDeletedEvent(id));
        eventPublisher.publishEvent(new MenuChangeEvent("deleted", id));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MenuItemImageResponse uploadImage(@NonNull Long id, @NonNull Long version, @NonNull MultipartFile file) {
        imageFileValidator.validate(file);

        // Pre-validate entity existence and version in a short read-only transaction before external upload.
        transactionExecutor.readOnly(() -> {
            MenuItem item = getEntityById(id);
            assertVersionMatches(item.getVersion(), version);
            return null;
        });

        String newUrl = uploadImageOutsideTransaction(id, file);

        try {
            return transactionExecutor.write(() -> {
                MenuItem item = getEntityById(id);
                assertVersionMatches(item.getVersion(), version);
                String oldUrl = item.getImg();

                item.setImg(newUrl);
                menuItemRepository.saveAndFlush(item);

                if (isCustomImage(oldUrl)) {
                    sideEffects.afterCommit(
                            () -> storageService.delete(oldUrl), "delete replaced menu item image " + id);
                }

                eventPublisher.publishEvent(new MenuChangeEvent("image_updated", id));
                return new MenuItemImageResponse(newUrl);
            });
        } catch (RuntimeException e) {
            deleteUploadedImageSilently(newUrl, id);
            throw e;
        }
    }

    private String uploadImageOutsideTransaction(Long id, MultipartFile file) {
        try {
            return storageService.upload(file, MENU_ITEM_IMAGE_FOLDER);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading menu item image ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload menu item image", e);
        }
    }

    private void deleteUploadedImageSilently(String uploadedUrl, Long id) {
        try {
            storageService.delete(uploadedUrl);
        } catch (Exception cleanupException) {
            log.warn("Failed to clean up rolled back menu item image ID {}: {}", id, cleanupException.getMessage());
        }
    }

    private MenuItem getEntityById(Long id) {
        return menuItemRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));
    }

    private MenuItem saveMenuItem(MenuItem item) {
        try {
            return menuItemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateMenuItemNameViolation(exception)) {
                throw new BusinessException(ErrorCode.MENU_ITEM_NAME_EXISTS, "Item name already exists", exception);
            }
            throw exception;
        }
    }

    private boolean isDuplicateMenuItemNameViolation(Throwable exception) {
        Throwable cursor = exception;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("ux_menu_item_name_active") || lower.contains("uk_menu_item_name_active")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private String normalizeBlank(String value) {
        return MenuTextSupport.normalizeBlank(value);
    }

    private String normalizeBlankOrDefault(String value, String defaultValue) {
        String normalized = MenuTextSupport.normalizeBlank(value);
        return normalized == null ? defaultValue : normalized;
    }

    private void assertVersionMatches(Long currentVersion, Long expectedVersion) {
        com.qros.shared.validation.OptimisticVersionGuard.requireMatch(
                currentVersion, expectedVersion, "Menu item", "menu item");
    }

    private boolean isCustomImage(String image) {
        return MenuTextSupport.isHttpImage(image) && !DEFAULT_MENU_ITEM_IMAGE.equals(image);
    }

    private MenuItemResponse applyAvailability(MenuItemResponse response, boolean effectiveAvailable) {
        if (response == null || Objects.equals(response.available(), effectiveAvailable)) {
            return response;
        }
        return menuItemMapper.withAvailability(response, effectiveAvailable);
    }
}
