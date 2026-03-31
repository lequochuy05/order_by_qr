package com.sacmauquan.qrordering.service;

import org.springframework.lang.NonNull;

import com.sacmauquan.qrordering.model.MenuItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MenuItemService {
    List<MenuItem> getAllMenuItems();

    List<MenuItem> getItemsByCategory(@NonNull Integer categoryId);

    Optional<MenuItem> getItemById(@NonNull Long id);

    MenuItem createItem(@NonNull MenuItem item);

    Optional<MenuItem> updateItem(@NonNull Long id, @NonNull MenuItem updated);

    void deleteItem(@NonNull Long id);

    Map<String, Object> uploadImage(@NonNull Long id, @NonNull MultipartFile file);
}
