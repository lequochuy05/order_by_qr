package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuItemController {

    @Autowired
    private MenuItemRepository menuItemRepository;

    // API trả về tất cả món
    @GetMapping
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    // API lọc theo categoryId
    @GetMapping("/category/{categoryId}")
    public List<MenuItem> getItemsByCategory(@PathVariable Integer categoryId) {
        return menuItemRepository.findByCategoryId(categoryId);
    }
}
