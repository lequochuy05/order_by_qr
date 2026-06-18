package com.qros.modules.menu.controller;

import com.qros.modules.menu.dto.request.CategoryRequest;
import com.qros.modules.menu.dto.response.CategoryResponse;
import com.qros.modules.menu.service.CategoryService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiRoutes.CATEGORIES)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/active")
    public ApiResponse<List<CategoryResponse>> getAllActive() {
        return ApiResponse.success(categoryService.getAllActive());
    }

    @GetMapping
    public ApiResponse<Page<CategoryResponse>> search(@RequestParam(required = false) String q, Pageable pageable) {
        return ApiResponse.success(categoryService.search(q, pageable));
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody @NonNull CategoryRequest req) {
        return ApiResponse.success("Category created successfully", categoryService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(
            @PathVariable @NonNull Long id, @Valid @RequestBody @NonNull CategoryRequest req) {
        return ApiResponse.success("Category updated successfully", categoryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        categoryService.delete(id);
        return ApiResponse.success("Category deleted successfully", null);
    }

    @PostMapping("/{id}/image")
    public ApiResponse<Map<String, String>> uploadImage(
            @PathVariable @NonNull Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Category image uploaded successfully", categoryService.uploadImage(id, file));
    }
}
