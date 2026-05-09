package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.MenuItemRequest;
import com.sacmauquan.qrordering.dto.MenuItemResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MenuItemService {
    List<MenuItemResponse> getAllMenuItems();

    List<MenuItemResponse> getItemsByCategory(@NonNull Integer categoryId);

    MenuItemResponse getItemById(@NonNull Long id);

    MenuItemResponse createItem(@NonNull MenuItemRequest req);

    MenuItemResponse updateItem(@NonNull Long id, @NonNull MenuItemRequest req);

    void deleteItem(@NonNull Long id);

    Map<String, Object> uploadImage(@NonNull Long id, @NonNull MultipartFile file);
}
