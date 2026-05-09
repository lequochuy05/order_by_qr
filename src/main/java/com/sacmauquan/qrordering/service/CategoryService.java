package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.repository.CategoryRepository;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepo;
    private final NotificationService notificationService;
    private final ImageManagerService imageManager;

    @Cacheable(value = "categories", key = "'all_active'")
    public List<Category> getAllActive() {
        return categoryRepo.findAllActiveWithItems();
    }

    public Page<Category> search(String q, @NonNull Pageable pageable) {
        if (q == null || q.trim().isEmpty()) {
            return categoryRepo.findAll(pageable);
        }
        return categoryRepo.findByNameContainingIgnoreCase(q, pageable);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category create(@NonNull Category c) {
        if (categoryRepo.existsByNameIgnoreCase(c.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên danh mục đã tồn tại");
        }

        Category saved = categoryRepo.save(c);
        notificationService.notifyCategoryChange("created", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category update(@NonNull Integer id, @NonNull Category input) {
        Category exist = categoryRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));

        // Kiểm tra trùng tên
        if (categoryRepo.existsByNameIgnoreCaseAndIdNot(input.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên danh mục đã tồn tại");
        }

        exist.setName(input.getName());
        exist.setActive(input.getActive()); // Cập nhật trạng thái kinh doanh

        if (input.getImg() != null && !input.getImg().isBlank()) {
            exist.setImg(input.getImg());
        }

        Category saved = categoryRepo.save(exist);
        notificationService.notifyCategoryChange("updated", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(@NonNull Integer id) {
        Category cat = categoryRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));

        // Xử lý ảnh nếu có trước khi xóa
        if (cat.getImg() != null && !cat.getImg().isBlank()) {
            try {
                imageManager.delete(cat.getImg());
            } catch (Exception e) {
                log.error("Lỗi xóa ảnh danh mục ID {}: {}", id, e.getMessage());
            }
        }

        categoryRepo.delete(cat);
        notificationService.notifyCategoryChange("deleted", id);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Map<String, String> uploadImage(@NonNull Integer id, @NonNull MultipartFile file) {
        Category cat = categoryRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File không hợp lệ");
        }

        try {
            String newUrl = imageManager.replace(file, cat.getImg(), "order_by_qr/categories");
            cat.setImg(newUrl);
            categoryRepo.save(cat);
            notificationService.notifyCategoryChange("image_updated", id);
            return Map.of("img", newUrl);
        } catch (Exception e) {
            log.error("Lỗi upload ảnh cho danh mục ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải ảnh lên");
        }
    }
}
