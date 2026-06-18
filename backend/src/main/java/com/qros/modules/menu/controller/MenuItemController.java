package com.qros.modules.menu.controller;

import com.qros.modules.menu.dto.request.MenuItemRequest;
import com.qros.modules.menu.dto.response.MenuItemResponse;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
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
            Pageable pageable) {
        return ApiResponse.success(menuItemService.searchManagementSummary(q, categoryId, pageable));
    }

    @GetMapping("/management-summary")
    public ApiResponse<Page<MenuItemResponse>> getManagementSummary(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        return ApiResponse.success(menuItemService.searchManagementSummary(q, categoryId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuItemResponse> getById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(menuItemService.getById(id));
    }

    @PostMapping
    public ApiResponse<MenuItemResponse> create(@Valid @RequestBody @NonNull MenuItemRequest req) {
        return ApiResponse.success("Menu item created successfully", menuItemService.create(req));
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
    public ApiResponse<Map<String, String>> uploadImage(
            @PathVariable @NonNull Long id, @RequestParam("file") @NonNull MultipartFile file) {
        return ApiResponse.success("Menu item image uploaded successfully", menuItemService.uploadImage(id, file));
    }
}
