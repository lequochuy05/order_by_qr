package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.MenuItemRequest;
import com.sacmauquan.qrordering.dto.MenuItemResponse;
import com.sacmauquan.qrordering.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * MenuItemController - Quản lý thực đơn món ăn.
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    /**
     * Lấy toàn bộ thực đơn
     */
    @GetMapping
    public ApiResponse<List<MenuItemResponse>> getAllMenuItems() {
        return ApiResponse.success(menuItemService.getAllMenuItems());
    }

    /**
     * Lấy danh sách món ăn theo từng danh mục cụ thể
     */
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<MenuItemResponse>> getItemsByCategory(@PathVariable @NonNull Integer categoryId) {
        return ApiResponse.success(menuItemService.getItemsByCategory(categoryId));
    }

    /**
     * Lấy thông tin chi tiết một món ăn kèm theo các Options/Toppings
     */
    @GetMapping("/{id}")
    public ApiResponse<MenuItemResponse> getItemById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(menuItemService.getItemById(id));
    }

    /**
     * Thêm món ăn mới vào thực đơn
     */
    @PostMapping
    public ApiResponse<MenuItemResponse> createItem(@Valid @RequestBody @NonNull MenuItemRequest req) {
        return ApiResponse.success("Thêm món mới thành công", menuItemService.createItem(req));
    }

    /**
     * Cập nhật thông tin món ăn và đồng bộ danh sách tùy chọn (Options)
     */
    @PutMapping("/{id}")
    public ApiResponse<MenuItemResponse> updateItem(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull MenuItemRequest req) {
        return ApiResponse.success("Cập nhật món ăn thành công", menuItemService.updateItem(id, req));
    }

    /**
     * Xóa món ăn khỏi thực đơn
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable @NonNull Long id) {
        menuItemService.deleteItem(id);
        return ApiResponse.success("Xóa món ăn thành công", null);
    }

    /**
     * Tải lên hoặc cập nhật ảnh đại diện cho món ăn
     */
    @PostMapping("/{id}/image")
    public ApiResponse<Map<String, Object>> uploadImage(@PathVariable @NonNull Long id,
            @RequestParam("file") @NonNull MultipartFile file) {
        return ApiResponse.success("Cập nhật ảnh thành công", menuItemService.uploadImage(id, file));
    }
}
