package com.sacmauquan.qrordering.service;

import java.util.*;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile; 

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate broker;
    private final ImageManagerService imageManager;

    // ===== Lấy danh sách =====
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> getItemsByCategory(Integer categoryId) {
        return menuItemRepository.findByCategoryId(categoryId);
    }

    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    // ===== Thêm mới =====
    public MenuItem createItem(MenuItem item) {
        if (menuItemRepository.existsByNameIgnoreCase(item.getName())) {
            throw new IllegalArgumentException("Tên món đã tồn tại");
        }
        MenuItem saved = menuItemRepository.save(item);
        broker.convertAndSend("/topic/menu",
            Map.of("event", "changed", "type", "create", "id", saved.getId()));
        return saved;
    }

    // ===== Cập nhật =====
    @Transactional
    public Optional<MenuItem> updateItem(Long id, MenuItem updated) {
        return menuItemRepository.findById(id).map(item -> {
            if (!item.getName().equalsIgnoreCase(updated.getName())) {
                if (menuItemRepository.existsByNameIgnoreCase(updated.getName())) {
                    throw new IllegalArgumentException("Tên món đã tồn tại");
                }
            }

            item.setName(updated.getName());
            item.setPrice(updated.getPrice());
            item.setCategory(updated.getCategory());
            if (updated.getImg() != null && !updated.getImg().isBlank()) {
                item.setImg(updated.getImg());
            }

            MenuItem saved = menuItemRepository.save(item);
            broker.convertAndSend("/topic/menu",
                Map.of("event", "changed", "type", "update", "id", saved.getId()));
            return saved;
        });
    }

    // ===== Xóa món ăn =====
    public void deleteItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món ăn"));

        // Xóa ảnh khỏi Cloudinary nếu có
        if (item.getImg() != null && !item.getImg().isBlank()) {
            imageManager.delete(item.getImg());
        }

        menuItemRepository.delete(item);
        broker.convertAndSend("/topic/menu",
            Map.of("event", "changed", "type", "delete", "id", id));
    }

    // ===== Upload ảnh =====
    public Map<String, Object> uploadImage(Long id, MultipartFile file) {
        MenuItem item = menuItemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng hoặc không hợp lệ");
        }

        try {
            // Upload ảnh mới và xóa ảnh cũ
            String newUrl = imageManager.replace(file, item.getImg(), "order_by_qr/menu_items");
            item.setImg(newUrl);
            menuItemRepository.save(item);

            broker.convertAndSend("/topic/menu",
                Map.of("event", "changed", "type", "update", "id", item.getId()));

            return Map.of("img", newUrl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể tải ảnh lên: " + e.getMessage());
        }
    }
}
