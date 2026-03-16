package com.sacmauquan.qrordering.service;

import java.util.Map;
import java.util.List;
import java.util.NoSuchElementException;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;
    private final ApplicationEventPublisher eventPublisher; 
    private final ImageManagerService imageManager; 

    @Cacheable(value = "categories")
    public List<Category> getAll() {
        return repo.findAll();
    }

    public Page<Category> search(String q, Pageable pageable) {
        return repo.search(q, pageable);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category create(Category c) {
        if (repo.existsByNameIgnoreCase(c.getName()))
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        
        Category saved = repo.save(c);
        notifyChange("changed", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category update(Integer id, Category input) {
        Category exist = repo.findById(id)
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
        notifyChange("changed", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Integer id) {
        Category cat = repo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));

        // Xóa ảnh khỏi Cloudinary nếu có
        if (cat.getImg() != null && !cat.getImg().isEmpty()) {
            try {
                imageManager.delete(cat.getImg());
            } catch (Exception e) {
                System.err.println("Lỗi xóa ảnh Cloudinary: " + e.getMessage());
            }
        }

        repo.delete(cat);
        repo.flush();
        notifyChange("deleted", id);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Map<String, Object> uploadImage(Integer id, MultipartFile file) {
        Category cat = repo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));
            
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng hoặc không hợp lệ");
        }

        try {
            String newUrl = imageManager.replace(file, cat.getImg(), "order_by_qr/categories");
            cat.setImg(newUrl);
            repo.save(cat);
            notifyChange("changed image", id);
            return Map.of("img", newUrl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể tải ảnh lên: " + e.getMessage());
        }
    }

    /**
     * Gửi thông báo Realtime đồng nhất
     */
    private void notifyChange(String event, Object id) {
        eventPublisher.publishEvent(new com.sacmauquan.qrordering.event.WebSocketEvent(
                "/topic/categories", 
                "UPDATED", 
                "⚡ [WS] Category " + event + " -> Sent UPDATED signal"
        ));
    }
}