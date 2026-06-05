package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.MenuItemRequest;
import com.sacmauquan.qrordering.dto.MenuItemResponse;
import com.sacmauquan.qrordering.dto.CustomerPublicDto;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * MenuItemService - Interface for managing the menu catalog.
 * Handles item lifecycle, category association, and asset management.
 */
public interface MenuItemService {

    /**
     * Retrieves all menu items currently registered in the system.
     * 
     * @return List of all menu items
     */
    List<MenuItemResponse> getAllMenuItems();

    List<CustomerPublicDto.MenuItemItem> getPublicMenuItems();

    /**
     * Retrieves active menu items belonging to a specific category.
     * 
     * @param categoryId Target category ID
     * @return List of matching menu items
     */
    List<MenuItemResponse> getItemsByCategory(@NonNull Integer categoryId);

    List<CustomerPublicDto.MenuItemItem> getPublicItemsByCategory(@NonNull Integer categoryId);

    /**
     * Locates a single menu item by its identifier.
     * 
     * @param id Menu item ID
     * @return Detailed menu item response
     */
    MenuItemResponse getItemById(@NonNull Long id);

    /**
     * Registers a new menu item in the catalog.
     * 
     * @param req Item creation details
     * @return Created item response
     */
    MenuItemResponse createItem(@NonNull MenuItemRequest req);

    /**
     * Updates an existing menu item's configuration and properties.
     * 
     * @param id  Menu item ID
     * @param req Update request details
     * @return Updated item response
     */
    MenuItemResponse updateItem(@NonNull Long id, @NonNull MenuItemRequest req);

    /**
     * Soft deletes a menu item by deactivating it.
     * 
     * @param id Menu item ID
     */
    void deleteItem(@NonNull Long id);

    /**
     * Uploads and links a representative image to a menu item.
     * 
     * @param id   Menu item ID
     * @param file The image asset
     * @return Map containing upload metadata and URLs
     */
    Map<String, Object> uploadImage(@NonNull Long id, @NonNull MultipartFile file);
}
