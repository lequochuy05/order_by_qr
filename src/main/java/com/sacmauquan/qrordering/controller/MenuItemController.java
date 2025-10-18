package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.service.MenuItemService;
import com.sacmauquan.qrordering.service.ImageManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final MenuItemRepository repo;
    private final ImageManagerService imageManager;

    @GetMapping
    public List<MenuItem> getAllMenuItems() {
        return menuItemService.getAllMenuItems();
    }

    @GetMapping("/category/{categoryId}")
    public List<MenuItem> getItemsByCategory(@PathVariable Integer categoryId) {
        return menuItemService.getItemsByCategory(categoryId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItem> getItemById(@PathVariable Long id) {
        return menuItemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createItem(@RequestBody MenuItem item) {
        try {
            return ResponseEntity.ok(menuItemService.createItem(item));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody MenuItem updated) {
        try {
            return menuItemService.updateItem(id, updated)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        var itemOpt = repo.findById(id);
        if (itemOpt.isEmpty()) return ResponseEntity.notFound().build();

        var item = itemOpt.get();
        // 🧹 Xóa ảnh trên Cloudinary
        imageManager.delete(item.getImg());

        repo.delete(item);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            var item = repo.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món"));
            if (file.isEmpty()) return ResponseEntity.badRequest().body("File rỗng");

            // 🧩 Thay ảnh mới, tự xóa cũ
            String newUrl = imageManager.replace(file, item.getImg(), "order_by_qr/menu_items");
            item.setImg(newUrl);
            repo.save(item);

            return ResponseEntity.ok(Map.of("img", newUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
