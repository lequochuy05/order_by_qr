package com.qros.modules.menu.service;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.menu.dto.publicmenu.PublicCategoryItem;
import com.qros.modules.menu.dto.request.CategoryRequest;
import com.qros.modules.menu.dto.response.CategoryResponse;
import com.qros.modules.menu.mapper.CategoryMapper;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.repository.CategoryRepository;
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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private static final String CATEGORY_IMAGE_FOLDER = "order_by_qr/categories";

    private final CategoryRepository categoryRepo;
    private final MenuItemRepository menuItemRepository;
    private final CategoryMapper categoryMapper;
    private final PublicMenuMapper publicMenuMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageService storageService;
    private final TransactionSideEffectService sideEffects;

    @Cacheable(value = CacheNames.CATEGORIES, key = "'all_active'")
    public List<CategoryResponse> getAllActive() {
        return categoryRepo.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(categoryMapper::toSummaryResponse)
                .toList();
    }
    @Cacheable(value = CacheNames.CATEGORIES, key = "'public_all_active'")
    public List<PublicCategoryItem> getPublicActive() {
        return categoryRepo.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(publicMenuMapper::toCategoryItem)
                .toList();
    }


    public Page<CategoryResponse> search(String q, @NonNull Pageable pageable) {
        Page<Category> page = q == null || q.trim().isEmpty()
                ? categoryRepo.findAll(pageable)
                : categoryRepo.findByNameContainingIgnoreCase(q.trim(), pageable);

        return page.map(categoryMapper::toSummaryResponse);
    }

    @Transactional
    @CacheEvict(value = { CacheNames.CATEGORIES, CacheNames.MENU, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public CategoryResponse create(@NonNull CategoryRequest req) {
        String name = normalizeRequired(req.name(), "Category name cannot be empty");
        if (categoryRepo.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        Category category = Category.builder()
                .name(name)
                .img(normalizeBlank(req.img()))
                .description(normalizeBlank(req.description()))
                .active(req.active() != null ? req.active() : true)
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .build();

        Category saved = categoryRepo.save(category);
        eventPublisher.publishEvent(new CategoryChangeEvent("created", saved.getId()));

        return categoryMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = { CacheNames.CATEGORIES, CacheNames.MENU, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public CategoryResponse update(@NonNull Long id, @NonNull CategoryRequest req) {
        String name = normalizeRequired(req.name(), "Category name cannot be empty");
        Category category = getEntityById(id);

        if (!category.getName().equalsIgnoreCase(name)
                && categoryRepo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        category.setName(name);
        category.setImg(normalizeBlank(req.img()));
        category.setDescription(normalizeBlank(req.description()));

        if (req.active() != null) {
            category.setActive(req.active());
        }

        if (req.displayOrder() != null) {
            category.setDisplayOrder(req.displayOrder());
        }

        Category saved = categoryRepo.save(category);
        eventPublisher.publishEvent(new CategoryChangeEvent("updated", saved.getId()));

        return categoryMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = { CacheNames.CATEGORIES, CacheNames.MENU, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public void delete(@NonNull Long id) {
        Category category = getEntityById(id);

        if (menuItemRepository.countByCategoryIdAndActiveTrue(id) > 0) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "Cannot delete category that still contains active menu items");
        }

        String oldImg = category.getImg();

        categoryRepo.delete(category);

        if (isCustomImage(oldImg)) {
            sideEffects.afterCommit(
                    () -> storageService.delete(oldImg),
                    "delete category image after category delete " + id);
        }

        eventPublisher.publishEvent(new CategoryChangeEvent("deleted", id));
    }

    @Transactional
    @CacheEvict(value = { CacheNames.CATEGORIES, CacheNames.MENU, CacheNames.COMBOS, CacheNames.RECOMMENDATIONS, CacheNames.POPULAR_ITEMS }, allEntries = true)
    public Map<String, String> uploadImage(@NonNull Long id, @NonNull MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }

        Category category = getEntityById(id);
        String oldUrl = category.getImg();

        try {
            String newUrl = storageService.upload(file, CATEGORY_IMAGE_FOLDER);

            sideEffects.afterRollback(
                    () -> storageService.delete(newUrl),
                    "delete rolled back category image " + id);

            category.setImg(newUrl);
            categoryRepo.save(category);

            if (isCustomImage(oldUrl)) {
                sideEffects.afterCommit(
                        () -> storageService.delete(oldUrl),
                        "delete replaced category image " + id);
            }

            eventPublisher.publishEvent(new CategoryChangeEvent("image_updated", id));

            return Map.of("img", newUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading category image ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload image", e);
        }
    }

    private Category getEntityById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRequired(String value, String message) {
        String trimmed = normalizeBlank(value);
        if (trimmed == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return trimmed;
    }

    private boolean isCustomImage(String image) {
        return image != null
                && !image.isBlank()
                && image.startsWith("http");
    }
}
