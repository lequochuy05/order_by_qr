package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.dto.MenuItemRequest;
import com.sacmauquan.qrordering.dto.MenuItemResponse;
import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.model.ItemOption;
import com.sacmauquan.qrordering.model.ItemOptionValue;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.CategoryRepository;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.service.ImageManagerService;
import com.sacmauquan.qrordering.service.MenuItemService;
import com.sacmauquan.qrordering.service.NotificationService;
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
 * MenuItemServiceImpl - Quản lý thực đơn món ăn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;
    private final ImageManagerService imageManager;

    @Override
    @Cacheable(value = "menu", key = "'all'")
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "menu", key = "'category_' + #categoryId")
    public List<MenuItemResponse> getItemsByCategory(@NonNull Integer categoryId) {
        return menuItemRepository.findByCategoryIdAndActiveTrue(Objects.requireNonNull(categoryId)).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public MenuItemResponse getItemById(@NonNull Long id) {
        MenuItem item = menuItemRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn"));
        return convertToResponse(item);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemResponse createItem(@NonNull MenuItemRequest req) {
        if (menuItemRepository.existsByNameIgnoreCase(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên món ăn đã tồn tại");
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không hợp lệ"));

        MenuItem item = MenuItem.builder()
                .name(req.getName())
                .img(req.getImg())
                .price(req.getPrice())
                .active(req.getActive() != null ? req.getActive() : true)
                .category(category)
                .build();

        if (req.getItemOptions() != null) {
            req.getItemOptions().forEach(optReq -> {
                ItemOption opt = buildItemOption(optReq, item);
                item.getItemOptions().add(opt);
            });
        }

        MenuItem saved = menuItemRepository.save(item);
        notificationService.notifyMenuChange("create", saved.getId());
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemResponse updateItem(@NonNull Long id, @NonNull MenuItemRequest req) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn"));

        if (menuItemRepository.existsByNameIgnoreCaseAndIdNot(req.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên món ăn đã tồn tại");
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không hợp lệ"));

        item.setName(req.getName());
        item.setPrice(req.getPrice());
        item.setCategory(category);
        item.setActive(req.getActive() != null ? req.getActive() : item.getActive());
        if (req.getImg() != null) item.setImg(req.getImg());

        syncOptions(item, req.getItemOptions());

        MenuItem saved = menuItemRepository.save(item);
        notificationService.notifyMenuChange("update", saved.getId());
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public void deleteItem(@NonNull Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn"));

        imageManager.delete(item.getImg());
        menuItemRepository.delete(item);
        notificationService.notifyMenuChange("delete", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public Map<String, Object> uploadImage(@NonNull Long id, @NonNull MultipartFile file) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món"));

        try {
            String newUrl = imageManager.replace(file, item.getImg(), "order_by_qr/menu_items");
            item.setImg(newUrl);
            menuItemRepository.save(item);
            notificationService.notifyMenuChange("upload_image", id);
            return Map.of("img", newUrl);
        } catch (IOException e) {
            log.error("Lỗi upload ảnh cho món ăn {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi upload ảnh");
        }
    }

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

        // Xóa những option không còn trong list gửi lên
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
                                v.getId(), v.getName(), v.getExtraPrice()
                        )).toList()
                )).toList(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
