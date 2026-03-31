package com.sacmauquan.qrordering.service.impl;

import org.springframework.lang.NonNull;
import java.util.Objects;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.repository.CategoryRepository;
import com.sacmauquan.qrordering.service.CategoryService;
import com.sacmauquan.qrordering.service.ImageManagerService;
import com.sacmauquan.qrordering.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repo;
    private final NotificationService notificationService;
    private final ImageManagerService imageManager;

    @Override
    @Cacheable(value = "categories")
    public List<Category> getAll() {
        return repo.findAll();
    }

    @Override
    public Page<Category> search(String q, @NonNull Pageable pageable) {
        if (q == null || q.trim().isEmpty()) {
            return repo.findAll(pageable);
        } else {
            return repo.findByNameContainingIgnoreCase(q, pageable);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category create(@NonNull Category c) {
        if (repo.existsByNameIgnoreCase(Objects.requireNonNull(c.getName())))
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");

        Category saved = repo.save(c);
        notificationService.notifyCategoryChange("changed", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category update(@NonNull Integer id, @NonNull Category input) {
        Category exist = repo.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));

        if (!exist.getName().equalsIgnoreCase(input.getName())
                && repo.existsByNameIgnoreCase(input.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        exist.setName(input.getName());
        if (input.getImg() != null && !input.getImg().trim().isEmpty()) {
            exist.setImg(input.getImg());
        }

        Category saved = repo.save(exist);
        notificationService.notifyCategoryChange("changed", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(@NonNull Integer id) {
        Category cat = repo.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));

        if (cat.getImg() != null && !cat.getImg().isEmpty()) {
            try {
                imageManager.delete(cat.getImg());
            } catch (Exception e) {
                System.err.println("Lỗi xóa ảnh Cloudinary: " + e.getMessage());
            }
        }

        repo.delete(cat);
        repo.flush();
        notificationService.notifyCategoryChange("deleted", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Map<String, Object> uploadImage(@NonNull Integer id, @NonNull MultipartFile file) {
        Category cat = repo.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng hoặc không hợp lệ");
        }

        try {
            String newUrl = imageManager.replace(file, cat.getImg(), "order_by_qr/categories");
            cat.setImg(newUrl);
            repo.save(cat);
            notificationService.notifyCategoryChange("changed image", id);
            return Map.of("img", newUrl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể tải ảnh lên: " + e.getMessage());
        }
    }
}
