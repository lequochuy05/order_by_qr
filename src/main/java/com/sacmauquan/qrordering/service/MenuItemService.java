package com.sacmauquan.qrordering.service;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.NoSuchElementException;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile; 

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate broker;
    private final ImageManagerService imageManager;

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> getItemsByCategory(Integer categoryId) {
        return menuItemRepository.findByCategoryId(categoryId);
    }

    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    @Transactional
    public MenuItem createItem(MenuItem item) {
        if (menuItemRepository.existsByNameIgnoreCase(item.getName())) {
            throw new IllegalArgumentException("Tên món đã tồn tại");
        }
        MenuItem saved = menuItemRepository.save(item);
        notifyChange("create", saved.getId());
        return saved;
    }

    @Transactional
    public Optional<MenuItem> updateItem(Long id, MenuItem updated) {
        return menuItemRepository.findById(id).map(item -> {
            if (!item.getName().equalsIgnoreCase(updated.getName())) {
                if (menuItemRepository.existsByNameIgnoreCase(updated.getName())) {
                    throw new IllegalArgumentException("Tên món đã tồn tại");
                }
            }

            // CHỈ LẤY CÁC TRƯỜNG CÓ TRONG MODEL CỦA BẠN
            item.setName(updated.getName());
            item.setPrice(updated.getPrice());
            item.setCategory(updated.getCategory());
            
            if (updated.getImg() != null && !updated.getImg().isBlank()) {
                item.setImg(updated.getImg());
            }

            MenuItem saved = menuItemRepository.save(item);
            notifyChange("update", saved.getId());
            return saved;
        });
    }

    @Transactional
    public void deleteItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món ăn"));

        if (item.getImg() != null && !item.getImg().isBlank()) {
            try {
                imageManager.delete(item.getImg());
            } catch (Exception e) {
                System.err.println("Lỗi xóa ảnh: " + e.getMessage());
            }
        }

        menuItemRepository.delete(item);
        menuItemRepository.flush();
        notifyChange("delete", id);
    }

    @Transactional
    public Map<String, Object> uploadImage(Long id, MultipartFile file) {
        MenuItem item = menuItemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng");
        }

        try {
            String newUrl = imageManager.replace(file, item.getImg(), "order_by_qr/menu_items");
            item.setImg(newUrl);
            menuItemRepository.save(item);

            return Map.of("img", newUrl);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload: " + e.getMessage());
        }
    }

    private void notifyChange(String type, Object id) {
        try {
            broker.convertAndSend("/topic/menu", "UPDATED");
            
            // Log ra console server để dễ debug
            System.out.println("⚡ [WS] Menu thay đổi (" + type + " ID: " + id + ")");
        } catch (MessagingException e) {
            System.err.println("Lỗi gửi WebSocket: " + e.getMessage());
            e.printStackTrace();
        }
    }
}