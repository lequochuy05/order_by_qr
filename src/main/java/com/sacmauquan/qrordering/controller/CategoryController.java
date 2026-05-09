package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * CategoryController - Quản lý danh mục món ăn.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    /**
     * Lấy danh sách danh mục hoạt động
     */
    @GetMapping
    public ApiResponse<List<Category>> findAll() {
        return ApiResponse.success(service.getAllActive());
    }

    /**
     * Tìm kiếm và phân trang danh mục
     */
    @GetMapping("/search")
    public ApiResponse<Page<Category>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 15, sort = "id") @NonNull Pageable pageable) {
        return ApiResponse.success(service.search(q, pageable));
    }

    /**
     * Tạo mới danh mục
     */
    @PostMapping
    public ApiResponse<Category> create(@Valid @RequestBody @NonNull Category category) {
        return ApiResponse.success("Tạo danh mục thành công", service.create(category));
    }

    /**
     * Cập nhật thông tin danh mục
     */
    @PutMapping("/{id}")
    public ApiResponse<Category> update(@PathVariable @NonNull Integer id,
            @Valid @RequestBody @NonNull Category category) {
        return ApiResponse.success("Cập nhật danh mục thành công", service.update(id, category));
    }

    /**
     * Xóa danh mục
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Integer id) {
        service.delete(id);
        return ApiResponse.success("Xóa danh mục thành công", null);
    }

    /**
     * Tải lên ảnh đại diện cho danh mục
     */
    @PostMapping("/{id}/image")
    public ApiResponse<Map<String, String>> uploadImage(@PathVariable @NonNull Integer id,
            @RequestParam("file") @NonNull MultipartFile file) {
        return ApiResponse.success("Tải ảnh lên thành công", service.uploadImage(id, file));
    }
}
