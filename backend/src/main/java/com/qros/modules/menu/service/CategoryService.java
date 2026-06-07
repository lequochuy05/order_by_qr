package com.qros.modules.menu.service;

import com.qros.infrastructure.storage.CloudinaryStorageService;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.menu.dto.CategoryRequest;
import com.qros.modules.menu.dto.CategoryResponse;
import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.repository.CategoryRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * CategoryService - Service for managing menu categories.
 * Handles category lifecycle, imagery, and caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepo;
    private final NotificationService notificationService;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final TransactionSideEffectService sideEffects;

    /**
     * Retrieves all active categories along with their associated menu items.
     * Uses caching to optimize frontend menu displays.
     * 
     * @return List of active CategoryResponse DTOs
     */
    @Cacheable(value = "categories", key = "'all_active'")
    public List<CategoryResponse> getAllActive() {
        return categoryRepo.findAllActiveWithItems().stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Cacheable(value = "categories", key = "'public_all_active'")
    public List<PublicMenuResponse.CategoryItem> getPublicActive() {
        return categoryRepo.findAllActiveWithItems().stream()
                .map(PublicMenuResponse::fromCategory)
                .toList();
    }

    /**
     * Searches for categories by name with pagination support.
     * 
     * @param q        Search keyword
     * @param pageable Pagination and sorting information
     * @return Paged result of matching CategoryResponse DTOs
     */
    public Page<CategoryResponse> search(String q, @NonNull Pageable pageable) {
        Page<Category> page;
        if (q == null || q.trim().isEmpty()) {
            page = categoryRepo.findAll(pageable);
        } else {
            page = categoryRepo.findByNameContainingIgnoreCase(q, pageable);
        }
        return page.map(this::convertToResponse);
    }

    /**
     * Creates a new category and invalidates the category cache.
     * 
     * @param c Category entity to create
     * @return Saved CategoryResponse DTO
     * @throws BusinessException if category name already exists
     */
    @Transactional
    @CacheEvict(value = { "categories", "menu", "recommendations", "popularItems" }, allEntries = true)
    public CategoryResponse create(@NonNull CategoryRequest input) {
        if (categoryRepo.existsByNameIncludingDeleted(input.getName())) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        Category c = new Category();
        c.setName(input.getName());
        c.setActive(input.getActive() != null ? input.getActive() : true);
        if (input.getImg() != null) {
            c.setImg(input.getImg());
        }

        Category saved = categoryRepo.save(c);
        notificationService.notifyCategoryChange("created", saved.getId());
        return convertToResponse(saved);
    }

    /**
     * Updates an existing category's properties.
     * 
     * @param id    Category ID
     * @param input Updated category details
     * @return Updated CategoryResponse DTO
     */
    @Transactional
    @CacheEvict(value = { "categories", "menu", "recommendations", "popularItems" }, allEntries = true)
    public CategoryResponse update(@NonNull Integer id, @NonNull CategoryRequest input) {
        Category exist = categoryRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!exist.getName().equalsIgnoreCase(input.getName())
                && categoryRepo.existsByNameIncludingDeleted(input.getName())) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        exist.setName(input.getName());
        if (input.getActive() != null) {
            exist.setActive(input.getActive());
        }

        if (input.getImg() != null && !input.getImg().isBlank()) {
            exist.setImg(input.getImg());
        }

        Category saved = categoryRepo.save(exist);
        notificationService.notifyCategoryChange("updated", saved.getId());
        return convertToResponse(saved);
    }

    /**
     * Soft deletes a category and cleans up its associated cloud image.
     * 
     * @param id Category ID
     */
    @Transactional
    @CacheEvict(value = { "categories", "menu", "recommendations", "popularItems" }, allEntries = true)
    public void delete(@NonNull Integer id) {
        Category cat = categoryRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (cat.getImg() != null && !cat.getImg().isBlank()) {
            String oldImg = cat.getImg();
            sideEffects.afterCommit(() -> cloudinaryStorageService.delete(oldImg),
                    "delete category image after category delete " + id);
        }

        categoryRepo.delete(cat);
        notificationService.notifyCategoryChange("deleted", id);
    }

    /**
     * Replaces the representative image for a category.
     * 
     * @param id   Category ID
     * @param file The new image file
     * @return Map containing the new image URL
     */
    @Transactional
    @CacheEvict(value = { "categories", "menu", "recommendations", "popularItems" }, allEntries = true)
    public Map<String, String> uploadImage(@NonNull Integer id, @NonNull MultipartFile file) {
        Category cat = categoryRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }

        try {
            String oldUrl = cat.getImg();
            String newUrl = cloudinaryStorageService.upload(file, "order_by_qr/categories");
            sideEffects.afterRollback(() -> cloudinaryStorageService.delete(newUrl),
                    "delete rolled back category image " + id);
            if (oldUrl != null && !oldUrl.isBlank()) {
                sideEffects.afterCommit(() -> cloudinaryStorageService.delete(oldUrl),
                        "delete replaced category image " + id);
            }
            cat.setImg(newUrl);
            categoryRepo.save(cat);
            notificationService.notifyCategoryChange("image_updated", id);
            return Map.of("img", newUrl);
        } catch (Exception e) {
            log.error("Error uploading category image ID {}: {}", id, e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload image", e);
        }
    }

    /**
     * Mapping helper to convert Category entity to CategoryResponse DTO.
     */
    private CategoryResponse convertToResponse(Category category) {
        com.qros.modules.menu.dto.MenuItemResponse.CategorySummary catSummary = new com.qros.modules.menu.dto.MenuItemResponse.CategorySummary(
                category.getId(), category.getName());

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .img(category.getImg())
                .active(category.getActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .menuItems(
                        (!org.hibernate.Hibernate.isInitialized(category.getMenuItems())
                                || category.getMenuItems() == null)
                                        ? java.util.Collections.emptyList()
                                        : category.getMenuItems().stream()
                                                .map(item -> new com.qros.modules.menu.dto.MenuItemResponse(
                                                        item.getId(),
                                                        item.getName(),
                                                        item.getImg(),
                                                        item.getPrice(),
                                                        item.getActive(),
                                                        catSummary,
                                                        item.getItemOptions() == null
                                                                ? java.util.Collections.emptyList()
                                                                : item.getItemOptions().stream().map(
                                                                        o -> new com.qros.modules.menu.dto.MenuItemResponse.ItemOptionResponse(
                                                                                o.getId(),
                                                                                o.getName(),
                                                                                o.isRequired(),
                                                                                o.getMaxSelection(),
                                                                                o.getOptionValues() == null
                                                                                        ? java.util.Collections
                                                                                                .emptyList()
                                                                                        : o.getOptionValues().stream()
                                                                                                .map(v -> new com.qros.modules.menu.dto.MenuItemResponse.ItemOptionValueResponse(
                                                                                                        v.getId(),
                                                                                                        v.getName(),
                                                                                                        v.getExtraPrice()))
                                                                                                .toList()))
                                                                        .toList(),
                                                        item.getCreatedAt(),
                                                        item.getUpdatedAt()))
                                                .toList())
                .build();
    }
}
