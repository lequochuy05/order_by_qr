package com.sacmauquan.qrordering.service;

import java.io.IOException;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MenuItemService {

    @Autowired
    private MenuItemRepository menuItemRepository;
    @Autowired
    private SimpMessagingTemplate broker;

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> getItemsByCategory(Integer categoryId) {
        return menuItemRepository.findByCategoryId(categoryId);
    }

    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    public MenuItem createItem(MenuItem item) {
        if (menuItemRepository.existsByNameIgnoreCase(item.getName())) {
            throw new IllegalArgumentException("Tên món đã tồn tại");
        }
        MenuItem saved = menuItemRepository.save(item);
        // thông báo menu thay đổi
        broker.convertAndSend("/topic/menu", Map.of("event","changed","type","create","id", saved.getId()));
        return saved;
    }

    @Transactional
   public Optional<MenuItem> updateItem(Long id, MenuItem updated) {
        return menuItemRepository.findById(id).map(item -> {
            // chỉ kiểm tra trùng nếu đổi tên
            if (!item.getName().equalsIgnoreCase(updated.getName())) {
                if (menuItemRepository.existsByNameIgnoreCase(updated.getName())) {
                    throw new IllegalArgumentException("Tên món đã tồn tại");
                }
            }
            item.setName(updated.getName());
            item.setPrice(updated.getPrice());
            
            item.setCategory(updated.getCategory());

            // Nếu có ảnh mới
            if (updated.getImg() != null && !updated.getImg().trim().isEmpty()
                    && !updated.getImg().equals(item.getImg())) {

                // Xóa ảnh cũ (nếu tồn tại và là file trong uploads)
                if (item.getImg() != null && item.getImg().startsWith("/uploads/")) {
                    Path oldPath = Paths.get("uploads", item.getImg().replace("/uploads/", ""));
                    try {
                        Files.deleteIfExists(oldPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Gán ảnh mới
                item.setImg(updated.getImg());
            }

            MenuItem saved = menuItemRepository.save(item);

            // thông báo menu thay đổi
            broker.convertAndSend("/topic/menu", Map.of("event","changed","type","update","id", saved.getId()));
            return saved;
        });
    }

    public boolean deleteItem(Long id) {
        if (menuItemRepository.existsById(id)) {
            menuItemRepository.deleteById(id);
            // thông báo menu thay đổi
            broker.convertAndSend("/topic/menu", Map.of("event","changed","type","delete","id", id));
            return true;
        }
        return false;
    }
}
