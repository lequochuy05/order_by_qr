package com.qros.modules.menu.service;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.menu.dto.publicmenu.PublicCategoryItem;
import com.qros.modules.menu.dto.request.CategoryRequest;
import com.qros.modules.menu.dto.response.CategoryImageResponse;
import com.qros.modules.menu.dto.response.CategoryResponse;
import com.qros.modules.menu.mapper.CategoryMapper;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionExecutor;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.validation.ImageFileValidator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class CategoryService {

    private static final String CATEGORY_IMAGE_FOLDER = "order_by_qr/categories";

    private final CategoryRepository categoryRepo;
    private final MenuItemRepository menuItemRepository;
    private final CategoryMapper categoryMapper;
    private final PublicMenuMapper publicMenuMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageService storageService;
    private final TransactionSideEffectService sideEffects;
    private final ImageFileValidator imageFileValidator;
    private final TransactionExecutor transactionExecutor;
    private final MenuInventoryAvailabilityPort inventoryAvailabilityService;

    @Cacheable(value = CacheNames.CATEGORIES, key = "'all_active'")
    public List<CategoryResponse> getAllActive() {
        List<Category> categories = categoryRepo.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        Map<Long, Long> itemCounts =
                countItemsByCategoryIds(categories.stream().map(Category::getId).toList());
        return categories.stream()
                .map(category ->
                        categoryMapper.toSummaryResponse(category, itemCounts.getOrDefault(category.getId(), 0L)))
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

        Map<Long, Long> itemCounts = countItemsByCategoryIds(
                page.getContent().stream().map(Category::getId).toList());

        return page.map(
                category -> categoryMapper.toSummaryResponse(category, itemCounts.getOrDefault(category.getId(), 0L)));
    }

    public CategoryResponse getById(@NonNull Long id) {
        Category category = getEntityById(id);
        return categoryMapper.toSummaryResponse(category, menuItemRepository.countByCategoryId(id));
    }

    @Transactional
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

        Category saved = saveCategory(category);
        eventPublisher.publishEvent(new CategoryChangeEvent("created", saved.getId()));

        // N+1 fix: toSummaryResponse avoids lazy-loading the full menuItems collection.
        // The admin list view never needs menuItems on create/update response.
        return categoryMapper.toSummaryResponse(saved);
    }

    @Transactional
    public CategoryResponse update(@NonNull Long id, @NonNull CategoryRequest req) {
        String name = normalizeRequired(req.name(), "Category name cannot be empty");
        Category category = getEntityById(id);
        assertVersionMatches(category.getVersion(), req.version());

        if (!category.getName().equalsIgnoreCase(name) && categoryRepo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        category.setName(name);
        category.setImg(normalizeBlank(req.img()));
        category.setDescription(normalizeBlank(req.description()));

        Boolean previousActive = category.getActive();
        if (req.active() != null) {
            category.setActive(req.active());
        }

        if (req.displayOrder() != null) {
            category.setDisplayOrder(req.displayOrder());
        }

        Category saved = saveCategory(category);
        if (req.active() != null && !req.active().equals(previousActive)) {
            inventoryAvailabilityService.syncCombosForMenuItems(menuItemRepository.findByCategoryId(saved.getId()));
        }
        eventPublisher.publishEvent(new CategoryChangeEvent("updated", saved.getId()));

        // N+1 fix: toSummaryResponse avoids lazy-loading the full menuItems collection.
        return categoryMapper.toSummaryResponse(saved, menuItemRepository.countByCategoryId(saved.getId()));
    }

    @Transactional
    public void delete(@NonNull Long id, @NonNull Long version) {
        // E+L-fix: assert version before deleting to prevent concurrent-delete race conditions.
        // Two admins could simultaneously open the category list; without this guard, both would
        // succeed, resulting in a silent double-delete and a confusing 404 for the second one.
        Category category = getEntityById(id);
        assertVersionMatches(category.getVersion(), version);

        if (menuItemRepository.existsByCategoryId(id)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, "Cannot delete category that still contains menu items");
        }

        String oldImg = category.getImg();

        categoryRepo.delete(category);

        if (isCustomImage(oldImg)) {
            sideEffects.afterCommit(
                    () -> storageService.delete(oldImg), "delete category image after category delete " + id);
        }

        eventPublisher.publishEvent(new CategoryChangeEvent("deleted", id));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CategoryImageResponse uploadImage(@NonNull Long id, @NonNull Long version, @NonNull MultipartFile file) {
        imageFileValidator.validate(file);

        // Pre-validate entity existence and optimistic version in a short read-only transaction
        // so we fail fast before spending network I/O and never hold a JDBC connection during upload.
        transactionExecutor.readOnly(() -> {
            Category category = getEntityById(id);
            assertVersionMatches(category.getVersion(), version);
            return null;
        });

        String newUrl = uploadImageOutsideTransaction(id, file);

        try {
            return transactionExecutor.write(() -> {
                Category category = getEntityById(id);
                assertVersionMatches(category.getVersion(), version);
                String oldUrl = category.getImg();

                category.setImg(newUrl);
                categoryRepo.saveAndFlush(category);

                if (isCustomImage(oldUrl)) {
                    sideEffects.afterCommit(
                            () -> storageService.delete(oldUrl), "delete replaced category image " + id);
                }

                eventPublisher.publishEvent(new CategoryChangeEvent("image_updated", id));
                return new CategoryImageResponse(newUrl);
            });
        } catch (RuntimeException e) {
            deleteUploadedImageSilently(newUrl, id);
            throw e;
        }
    }

    private String uploadImageOutsideTransaction(Long id, MultipartFile file) {
        try {
            return storageService.upload(file, CATEGORY_IMAGE_FOLDER);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading category image ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload image", e);
        }
    }

    private void deleteUploadedImageSilently(String uploadedUrl, Long id) {
        try {
            storageService.delete(uploadedUrl);
        } catch (Exception cleanupException) {
            log.warn("Failed to clean up rolled back category image ID {}: {}", id, cleanupException.getMessage());
        }
    }

    private Category getEntityById(Long id) {
        return categoryRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Map<Long, Long> countItemsByCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : menuItemRepository.countByCategoryIdIn(categoryIds)) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Category saveCategory(Category category) {
        try {
            return categoryRepo.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS, "Category name already exists", exception);
        }
    }

    private String normalizeBlank(String value) {
        return MenuTextSupport.normalizeBlank(value);
    }

    private String normalizeRequired(String value, String message) {
        return MenuTextSupport.normalizeRequired(value, message);
    }

    private void assertVersionMatches(Long currentVersion, Long expectedVersion) {
        com.qros.shared.validation.OptimisticVersionGuard.requireMatch(
                currentVersion, expectedVersion, "Category", "category");
    }

    private boolean isCustomImage(String image) {
        return MenuTextSupport.isHttpImage(image);
    }
}
