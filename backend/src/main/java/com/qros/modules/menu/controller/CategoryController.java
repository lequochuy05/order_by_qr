package com.qros.modules.menu.controller;

import com.qros.modules.menu.dto.request.CategoryRequest;
import com.qros.modules.menu.dto.response.CategoryImageResponse;
import com.qros.modules.menu.dto.response.CategoryResponse;
import com.qros.modules.menu.service.CategoryService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    public ApiResponse<Page<CategoryResponse>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(
                            size = 24,
                            sort = {"displayOrder", "name"},
                            direction = Direction.ASC)
                    Pageable pageable) {
        return ApiResponse.success(categoryService.search(q, pageable));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody @NonNull CategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(), "Category created successfully", categoryService.create(req)));
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
    public ApiResponse<CategoryImageResponse> uploadImage(
            @PathVariable @NonNull Long id, @RequestParam("file") @NonNull MultipartFile file) {
        return ApiResponse.success("Category image uploaded successfully", categoryService.uploadImage(id, file));
    }
}
