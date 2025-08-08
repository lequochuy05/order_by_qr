package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MenuItemService {

    @Autowired
    private MenuItemRepository menuItemRepository;

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
        return menuItemRepository.save(item);
    }

    public Optional<MenuItem> updateItem(Long id, MenuItem updated) {
        return menuItemRepository.findById(id).map(item -> {
            item.setName(updated.getName());
            item.setPrice(updated.getPrice());
            item.setImg(updated.getImg());
            item.setCategory(updated.getCategory());
            return menuItemRepository.save(item);
        });
    }

    public boolean deleteItem(Long id) {
        if (menuItemRepository.existsById(id)) {
            menuItemRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
