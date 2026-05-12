package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.CategoryResponse;
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
 * CategoryController - Manages food and beverage categories.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    /**
     * Retrieves a list of all active categories.
     * 
     * @return List of active CategoryResponse objects
     */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> findAll() {
        return ApiResponse.success(service.getAllActive());
    }

    /**
     * Searches and paginates through categories based on a keyword.
     * 
     * @param q        Search keyword
     * @param pageable Pagination and sorting data
     * @return Page of CategoryResponse objects
     */
    @GetMapping("/search")
    public ApiResponse<Page<CategoryResponse>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 15, sort = "id") @NonNull Pageable pageable) {
        return ApiResponse.success(service.search(q, pageable));
    }

    /**
     * Creates a new category.
     * 
     * @param category Data for the new category
     * @return Created CategoryResponse object
     */
    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody @NonNull Category category) {
        return ApiResponse.success("Category created successfully", service.create(category));
    }

    /**
     * Updates an existing category's information.
     * 
     * @param id       Category ID
     * @param category Updated category data
     * @return Updated CategoryResponse object
     */
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable @NonNull Integer id,
            @Valid @RequestBody @NonNull Category category) {
        return ApiResponse.success("Category updated successfully", service.update(id, category));
    }

    /**
     * Deletes a category from the system.
     * 
     * @param id Category ID to delete
     * @return Void success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Integer id) {
        service.delete(id);
        return ApiResponse.success("Category deleted successfully", null);
    }

    /**
     * Uploads or updates the representative image for a category.
     * 
     * @param id   Category ID
     * @param file Image file to upload
     * @return Map containing the upload result (e.g., secure URL)
     */
    @PostMapping("/{id}/image")
    public ApiResponse<Map<String, String>> uploadImage(@PathVariable @NonNull Integer id,
            @RequestParam("file") @NonNull MultipartFile file) {
        return ApiResponse.success("Image uploaded successfully", service.uploadImage(id, file));
    }
}
