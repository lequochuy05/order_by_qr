package com.qros.modules.menu.controller;

import com.qros.shared.response.ApiResponse;
import com.qros.modules.menu.dto.MenuItemRequest;
import com.qros.modules.menu.dto.MenuItemResponse;
import com.qros.modules.menu.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * MenuItemController - Manages the food and beverage menu items.
 */
@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    /**
     * Retrieves the entire menu.
     * 
     * @return List of all MenuItemResponse objects
     */
    @GetMapping
    public ApiResponse<List<MenuItemResponse>> getAllMenuItems(@RequestParam(required = false) Integer categoryId) {
        if (categoryId != null) {
            return ApiResponse.success(menuItemService.getItemsByCategory(categoryId));
        }
        return ApiResponse.success(menuItemService.getAllMenuItems());
    }

    /**
     * Retrieves menu items belonging to a specific category.
     * 
     * @param categoryId ID of the category
     * @return List of MenuItemResponse objects in the category
     */
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<MenuItemResponse>> getItemsByCategory(@PathVariable @NonNull Integer categoryId) {
        return ApiResponse.success(menuItemService.getItemsByCategory(categoryId));
    }

    /**
     * Retrieves detailed information of a menu item, including its options and
     * toppings.
     * 
     * @param id Menu item ID
     * @return MenuItemResponse object
     */
    @GetMapping("/{id}")
    public ApiResponse<MenuItemResponse> getItemById(@PathVariable @NonNull Long id) {
        return ApiResponse.success(menuItemService.getItemById(id));
    }

    /**
     * Adds a new menu item to the menu.
     * 
     * @param req Data for the new menu item
     * @return Created MenuItemResponse object
     */
    @PostMapping
    public ApiResponse<MenuItemResponse> createItem(@Valid @RequestBody @NonNull MenuItemRequest req) {
        return ApiResponse.success("Menu item added successfully", menuItemService.createItem(req));
    }

    /**
     * Updates an existing menu item and synchronizes its options/toppings.
     * 
     * @param id  ID of the menu item to update
     * @param req Updated menu item data
     * @return Updated MenuItemResponse object
     */
    @PutMapping("/{id}")
    public ApiResponse<MenuItemResponse> updateItem(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull MenuItemRequest req) {
        return ApiResponse.success("Menu item updated successfully", menuItemService.updateItem(id, req));
    }

    /**
     * Deletes a menu item from the menu.
     * 
     * @param id ID of the menu item to delete
     * @return Void success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable @NonNull Long id) {
        menuItemService.deleteItem(id);
        return ApiResponse.success("Menu item deleted successfully", null);
    }

    /**
     * Uploads or updates the image for a menu item.
     * 
     * @param id   Menu item ID
     * @param file Image file to upload
     * @return Map containing image upload results
     */
    @PostMapping("/{id}/image")
    public ApiResponse<Map<String, Object>> uploadImage(@PathVariable @NonNull Long id,
            @RequestParam("file") @NonNull MultipartFile file) {
        return ApiResponse.success("Image updated successfully", menuItemService.uploadImage(id, file));
    }
}
