package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.service.MenuItemService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;                                  

import java.util.UUID;    
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.sacmauquan.qrordering.repository.MenuItemRepository;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuItemController {

    @Autowired
    private MenuItemService menuItemService;
    @Autowired
    private MenuItemRepository menuItemRepository;

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
            return ResponseEntity.badRequest().body(e.getMessage()); // 400 + message
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody MenuItem updated) {
        try {
            return menuItemService.updateItem(id, updated)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 400 + message
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        if (menuItemService.deleteItem(id)) return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file) {
        try {
            var item = menuItemRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món"));

            if (file.isEmpty()) return ResponseEntity.badRequest().body("File rỗng");

            // Lưu ra thư mục ngoài (đã được map bởi WebConfig -> /uploads/**)
            Path dir = Paths.get("uploads/menu").toAbsolutePath();
            Files.createDirectories(dir);

            String origin = file.getOriginalFilename();
            String ext = (origin != null && origin.lastIndexOf('.') >= 0)
                    ? origin.substring(origin.lastIndexOf('.'))
                    : ".jpg";
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;

            Files.copy(file.getInputStream(), dir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);

            String publicPath = "/uploads/menu/" + filename; 
            item.setImg(publicPath);
            menuItemRepository.save(item);

            return ResponseEntity.ok(Map.of("img", publicPath));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
