package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.service.MenuItemService;
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
        return ResponseEntity.ok(menuItemService.createItem(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody MenuItem updated) {
        return menuItemService.updateItem(id, updated)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        menuItemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(menuItemService.uploadImage(id, file));
    }
}
