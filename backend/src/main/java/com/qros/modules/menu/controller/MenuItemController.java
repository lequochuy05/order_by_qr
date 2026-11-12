package com.qros.modules.menu.controller;

import com.qros.modules.menu.dto.request.MenuItemRequest;
import com.qros.modules.menu.dto.response.MenuItemImageResponse;
import com.qros.modules.menu.dto.response.MenuItemResponse;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
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

/**
 * MenuItemController - Manages food and beverage menu items.
 */
@RestController
@RequestMapping(ApiRoutes.MENU_ITEMS)
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping
    public ApiResponse<Page<MenuItemResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(
                            size = 24,
                            sort = {"displayOrder", "name"},
                            direction = Direction.ASC)
                    Pageable pageable) {
        return ApiResponse.success(menuItemService.searchSummaries(q, categoryId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuItemResponse> getById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(menuItemService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemResponse>> create(@Valid @RequestBody @NonNull MenuItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(), "Menu item created successfully", menuItemService.create(req)));
    }

    @PutMapping("/{id}")
    public ApiResponse<MenuItemResponse> update(
            @PathVariable @NonNull Long id, @Valid @RequestBody @NonNull MenuItemRequest req) {
        return ApiResponse.success("Menu item updated successfully", menuItemService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        menuItemService.delete(id);
        return ApiResponse.success("Menu item deleted successfully", null);
    }

    @PostMapping("/{id}/image")
    public ApiResponse<MenuItemImageResponse> uploadImage(
            @PathVariable @NonNull Long id, @RequestParam("file") @NonNull MultipartFile file) {
        return ApiResponse.success("Menu item image uploaded successfully", menuItemService.uploadImage(id, file));
    }
}
