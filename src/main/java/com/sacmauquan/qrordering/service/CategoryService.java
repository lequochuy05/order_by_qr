package com.sacmauquan.qrordering.service;

import java.util.Map;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;
    private final SimpMessagingTemplate broker; 
    private final ImageManagerService imageManager; 

    public Page<Category> search(String q, Pageable pageable) {
      return repo.search(q, pageable);
    }

    public Category create(Category c) {
      if (repo.existsByNameIgnoreCase(c.getName()))
        throw new IllegalArgumentException("Tên danh mục đã tồn tại");
      
      Category saved = repo.save(c);
      broker.convertAndSend("/topic/categories",     // 2) GỬI SAU
          Map.of("event", "changed", "id", saved.getId()));
      return saved;
    }

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
      Category saved = repo.save(exist);             // LƯU TRƯỚC
      broker.convertAndSend("/topic/categories",
          Map.of("event","changed","id", saved.getId()));  // GỬI SAU
      return saved;
    }

    public void delete(Integer id) {
      Category cat = repo.findById(id)
          .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));

      // Xóa ảnh khỏi Cloudinary nếu có
      if (cat.getImg() != null && !cat.getImg().isEmpty()) {
          imageManager.delete(cat.getImg());
      }

      // Xóa khỏi database
      repo.delete(cat);

      // Gửi socket cập nhật realtime
      broker.convertAndSend("/topic/categories",
          Map.of("event", "deleted", "id", id));
  }


    public Map<String, Object> uploadImage(Integer id, MultipartFile file) {
      Category cat = repo.findById(id)
          .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));
      if (file == null || file.isEmpty()) {
          throw new IllegalArgumentException("File rỗng hoặc không hợp lệ");
      }

      try {
          // 🔹 Upload ảnh mới vào Cloudinary (tự động xóa ảnh cũ)
          String newUrl = imageManager.replace(file, cat.getImg(), "order_by_qr/categories");
          cat.setImg(newUrl);
          repo.save(cat);

          // 🔹 Gửi socket để cập nhật realtime nếu cần
          broker.convertAndSend("/topic/categories",
              Map.of("event", "changed", "id", cat.getId()));

          return Map.of("img", newUrl);
      } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("Không thể tải ảnh lên: " + e.getMessage());
      }
    }
}
