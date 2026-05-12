package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.MenuItem;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * MenuItemRepository - Repository interface for managing MenuItem entities.
 * Features optimized fetching using EntityGraph for related options and
 * categories.
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /**
     * Finds active menu items within a specific category.
     * 
     * @param cateId Category identifier
     * @return List of matching MenuItem entities
     */
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    List<MenuItem> findByCategoryIdAndActiveTrue(Integer cateId);

    /**
     * Checks if a menu item with the exact name already exists (case-insensitive).
     * 
     * @param name The name to check
     * @return true if exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks if another menu item already uses a specific name (case-insensitive).
     * 
     * @param name The name to check
     * @param id   The ID to exclude from search
     * @return true if another item with the same name exists
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Searches for menu items by name with partial match and pagination.
     * 
     * @param name     The search keyword
     * @param pageable Pagination and sorting information
     * @return Paged result of matching items
     */
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    Page<MenuItem> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Retrieves all menu items with full detail pre-fetching.
     * 
     * @return List of all menu items
     */
    @Override
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    @NonNull
    List<MenuItem> findAll();

    /**
     * Finds a menu item by ID with full detail pre-fetching.
     * 
     * @param id Item identifier
     * @return Optional containing the found item
     */
    @Override
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    @NonNull
    Optional<MenuItem> findById(@NonNull Long id);

    /**
     * Retrieves all menu items that are currently active and available for sale.
     * 
     * @return List of active menu items
     */
    @EntityGraph(attributePaths = { "category", "itemOptions", "itemOptions.optionValues" })
    List<MenuItem> findAllByActiveTrue();
}
