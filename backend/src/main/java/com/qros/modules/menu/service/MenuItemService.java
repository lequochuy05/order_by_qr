package com.qros.modules.menu.service;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.menu.dto.publicmenu.PublicMenuItem;
import com.qros.modules.menu.dto.request.ItemOptionRequest;
import com.qros.modules.menu.dto.request.ItemOptionValueRequest;
import com.qros.modules.menu.dto.request.MenuItemRequest;
import com.qros.modules.menu.dto.response.MenuItemResponse;
import com.qros.modules.menu.mapper.MenuItemMapper;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.ComboItemRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionSideEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final NotificationService notificationService;

    public List<MenuItemResponse> getAll() {
        return menuItemRepository.findAll().stream()
                .map(menuItemMapper::toResponse)
                .toList();
    }

    public List<MenuItemResponse> getAllManagementSummary() {
        return menuItemRepository.findAllForManagementSummary().stream()
                .map(menuItemMapper::toResponse)
                .toList();
    }

    public MenuItemResponse getById(@NonNull Long id) {
        return menuItemMapper.toResponse(getEntityById(id));
    }

    public List<MenuItemResponse> getByCategory(@NonNull Long categoryId) {
        return menuItemRepository.findByCategoryIdAndActiveTrueOrderByDisplayOrderAscNameAsc(categoryId).stream()
                .map(menuItemMapper::toResponse)
                .toList();
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

    @Transactional
    @CacheEvict(value = { CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public MenuItemResponse create(@NonNull MenuItemRequest req) {
        String name = normalizeRequired(req.name(), "Menu item name cannot be empty");

        if (menuItemRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NAME_EXISTS);
        }

        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        validateOptions(req.itemOptions());

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
            req.itemOptions().forEach(optionReq -> item.getItemOptions().add(buildItemOption(optionReq, item)));
        }

        MenuItem saved = menuItemRepository.save(item);
        notificationService.notifyMenuChange("created", saved.getId());

        return menuItemMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = { CacheNames.MENU, CacheNames.CATEGORIES, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public MenuItemResponse update(@NonNull Long id, @NonNull MenuItemRequest req) {
        MenuItem item = getEntityById(id);

        String name = normalizeRequired(req.name(), "Menu item name cannot be empty");

        if (!item.getName().equalsIgnoreCase(name)
                && menuItemRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NAME_EXISTS);
        }

        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        validateOptions(req.itemOptions());

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

        syncOptions(item, req.itemOptions());

        MenuItem saved = menuItemRepository.save(item);
        notificationService.notifyMenuChange("updated", saved.getId());

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

        notificationService.notifyMenuChange("deleted", id);
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

            notificationService.notifyMenuChange("image_updated", id);

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

    private ItemOption buildItemOption(ItemOptionRequest req, MenuItem item) {
        ItemOption option = ItemOption.builder()
                .name(normalizeRequired(req.name(), "Option name cannot be empty"))
                .required(req.required() != null ? req.required() : false)
                .maxSelection(req.maxSelection() != null ? req.maxSelection() : 1)
                .displayOrder(0)
                .menuItem(item)
                .build();

        if (req.optionValues() != null) {
            req.optionValues()
                    .forEach(valueReq -> option.getOptionValues().add(buildItemOptionValue(valueReq, option)));
        }

        return option;
    }

    private ItemOptionValue buildItemOptionValue(ItemOptionValueRequest req, ItemOption option) {
        return ItemOptionValue.builder()
                .name(normalizeRequired(req.name(), "Option value name cannot be empty"))
                .extraPrice(req.extraPrice() != null ? req.extraPrice() : BigDecimal.ZERO)
                .displayOrder(0)
                .itemOption(option)
                .build();
    }

    private void syncOptions(MenuItem item, List<ItemOptionRequest> incomingOptions) {
        item.getItemOptions().clear();

        if (incomingOptions == null || incomingOptions.isEmpty()) {
            return;
        }

        incomingOptions.forEach(optionReq -> item.getItemOptions().add(buildItemOption(optionReq, item)));
    }

    private void validateOptions(List<ItemOptionRequest> options) {
        if (options == null || options.isEmpty()) {
            return;
        }

        Set<String> optionNames = new HashSet<>();

        for (ItemOptionRequest option : options) {
            String optionName = normalizeRequired(option.name(), "Option name cannot be empty")
                    .toLowerCase();

            if (!optionNames.add(optionName)) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "Duplicate option name: " + option.name());
            }

            if (option.optionValues() == null || option.optionValues().isEmpty()) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "Option must contain at least one value");
            }

            if (option.maxSelection() != null && option.maxSelection() > option.optionValues().size()) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "Max selection cannot exceed option value count");
            }

            Set<String> valueNames = new HashSet<>();

            for (ItemOptionValueRequest value : option.optionValues()) {
                String valueName = normalizeRequired(value.name(), "Option value name cannot be empty")
                        .toLowerCase();

                if (!valueNames.add(valueName)) {
                    throw new BusinessException(
                            ErrorCode.BUSINESS_ERROR,
                            "Duplicate option value: " + value.name());
                }
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
