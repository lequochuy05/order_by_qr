package com.qros.modules.menu.service;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.menu.dto.publicmenu.PublicMenuItem;
import com.qros.modules.menu.dto.request.MenuItemRequest;
import com.qros.modules.menu.dto.response.MenuItemResponse;
import com.qros.modules.menu.mapper.MenuItemMapper;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.ComboItemRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionSideEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Page<MenuItemResponse> searchManagementSummary(String keyword, Long categoryId, @NonNull Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return menuItemRepository.searchManagementSummaries(normalizedKeyword, categoryId, pageable)
                .map(menuItemMapper::toSummaryResponse);
    }

    public MenuItemResponse getById(@NonNull Long id) {
        return menuItemMapper.toResponse(getEntityById(id));
    }

    @Cacheable(value = CacheNames.MENU, key = "'public_items'")
    public List<PublicMenuItem> getPublicMenuItems() {
        return menuItemRepository.findAllPublicAvailableItems().stream()
                .map(publicMenuMapper::toMenuItem)
                .toList();
    }

    @Cacheable(value = CacheNames.MENU, key = "'public_category_' + #categoryId")
    public List<PublicMenuItem> getPublicItemsByCategory(@NonNull Long categoryId) {
        return menuItemRepository.findPublicAvailableItemsByCategoryId(categoryId).stream()
                .map(publicMenuMapper::toMenuItem)
                .toList();
    }

    public List<PublicMenuItem> getPublicMenuItemsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Map<Long, PublicMenuItem> itemsById = new HashMap<>();
        menuItemRepository.findPublicAvailableItemsByIds(ids).stream()
                .map(publicMenuMapper::toMenuItem)
                .forEach(item -> itemsById.put(item.id(), item));

        return ids.stream()
                .map(itemsById::get)
                .filter(item -> item != null)
                .toList();
    }

    @Transactional
    @CacheEvict(value = { CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public MenuItemResponse create(@NonNull MenuItemRequest req) {
        String name = MenuItemOptionService.normalizeRequired(req.name(), "Menu item name cannot be empty");

        if (menuItemRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NAME_EXISTS);
        }

        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        menuItemOptionService.validateOptions(req.itemOptions());

        MenuItem item = MenuItem.builder()
                .name(name)
                .description(normalizeBlank(req.description()))
                .img(normalizeBlankOrDefault(req.img(), DEFAULT_MENU_ITEM_IMAGE))
                .price(req.price())
                .active(req.active() != null ? req.active() : true)
                .available(req.available() != null ? req.available() : true)
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .category(category)
                .build();

        if (req.itemOptions() != null) {
            req.itemOptions().forEach(optionReq -> item.getItemOptions().add(menuItemOptionService.buildItemOption(optionReq, item)));
        }

        MenuItem saved = menuItemRepository.save(item);
        eventPublisher.publishEvent(new MenuChangeEvent("created", saved.getId()));

        return menuItemMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = { CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public MenuItemResponse update(@NonNull Long id, @NonNull MenuItemRequest req) {
        MenuItem item = getEntityById(id);

        String name = MenuItemOptionService.normalizeRequired(req.name(), "Menu item name cannot be empty");

        if (!item.getName().equalsIgnoreCase(name)
                && menuItemRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NAME_EXISTS);
        }

        Category category = categoryRepository.findById(req.categoryId())
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
            item.setAvailable(req.available());
        }

        if (req.displayOrder() != null) {
            item.setDisplayOrder(req.displayOrder());
        }

        menuItemOptionService.syncOptions(item, req.itemOptions());

        MenuItem saved = menuItemRepository.save(item);
        eventPublisher.publishEvent(new MenuChangeEvent("updated", saved.getId()));

        return menuItemMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = { CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public void delete(@NonNull Long id) {
        MenuItem item = getEntityById(id);

        if (comboItemRepository.existsInActiveCombo(id)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "Cannot delete menu item that is part of an active combo");
        }

        String oldImg = item.getImg();

        menuItemRepository.delete(item);

        if (isCustomImage(oldImg)) {
            sideEffects.afterCommit(
                    () -> storageService.delete(oldImg),
                    "delete menu item image after item delete " + id);
        }

        eventPublisher.publishEvent(new MenuChangeEvent("deleted", id));
    }

    @Transactional
    @CacheEvict(value = { CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public Map<String, String> uploadImage(@NonNull Long id, @NonNull MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }

        MenuItem item = getEntityById(id);
        String oldUrl = item.getImg();

        try {
            String newUrl = storageService.upload(file, MENU_ITEM_IMAGE_FOLDER);

            sideEffects.afterRollback(
                    () -> storageService.delete(newUrl),
                    "delete rolled back menu item image " + id);

            item.setImg(newUrl);
            menuItemRepository.save(item);

            if (isCustomImage(oldUrl)) {
                sideEffects.afterCommit(
                        () -> storageService.delete(oldUrl),
                        "delete replaced menu item image " + id);
            }

            eventPublisher.publishEvent(new MenuChangeEvent("image_updated", id));

            return Map.of("img", newUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading menu item image ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload menu item image", e);
        }
    }

    private MenuItem getEntityById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeBlankOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private boolean isCustomImage(String image) {
        return image != null
                && !image.isBlank()
                && !DEFAULT_MENU_ITEM_IMAGE.equals(image)
                && image.startsWith("http");
    }
}
