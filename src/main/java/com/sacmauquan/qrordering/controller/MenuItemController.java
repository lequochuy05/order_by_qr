package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuItemController {

    @Autowired
    private MenuItemService menuItemService;

    @GetMapping
    public List<MenuItem> getAllMenuItems() {
        return menuItemService.getAllMenuItems();
    }

    @GetMapping("/category/{categoryId}")
    public List<MenuItem> getItemsByCategory(@PathVariable Integer categoryId) {
        return menuItemService.getItemsByCategory(categoryId);
    }

    // @GetMapping("/{id}")
    // public ResponseEntity<MenuItem> getItemById(@PathVariable Long id) {
    //     return menuItemService.getItemById(id)
    //             .map(ResponseEntity::ok)
    //             .orElse(ResponseEntity.notFound().build());
    // }

    // @PostMapping
    // public ResponseEntity<MenuItem> createItem(@RequestBody MenuItem item) {
    //     return ResponseEntity.ok(menuItemService.createItem(item));
    // }

    // @PutMapping("/{id}")
    // public ResponseEntity<MenuItem> updateItem(@PathVariable Long id, @RequestBody MenuItem updated) {
    //     return menuItemService.updateItem(id, updated)
    //             .map(ResponseEntity::ok)
    //             .orElse(ResponseEntity.notFound().build());
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<?> deleteItem(@PathVariable Long id) {
    //     if (menuItemService.deleteItem(id)) {
    //         return ResponseEntity.ok().build();
    //     } else {
    //         return ResponseEntity.notFound().build();
    //     }
    // }
}
