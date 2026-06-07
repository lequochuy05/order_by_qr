package com.qros.modules.menu.repository;

import com.qros.modules.menu.model.Category;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * CategoryRepository - Repository interface for managing Category entities.
 */
public interface CategoryRepository extends JpaRepository<Category, Integer> {

  /**
   * Checks if a category with the same name already exists (case-insensitive).
   * 
   * @param name The name to check
   * @return true if exists, false otherwise
   */
  boolean existsByNameIgnoreCase(String name);

  /**
   * Checks if another category already uses this name (case-insensitive).
   * 
   * @param name The name to check
   * @param id   The ID to exclude from search
   * @return true if another category exists with this name
   */
  boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

  /**
   * Retrieves all active categories along with their active menu items and
   * options using a single query.
   * 
   * @return List of active categories with pre-fetched associations
   */
  @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.menuItems m " +
      "LEFT JOIN FETCH m.itemOptions io " +
      "LEFT JOIN FETCH io.optionValues " +
      "WHERE c.active = true AND (m.active = true OR m.active IS NULL)")
  List<Category> findAllActiveWithItems();

  /**
   * Retrieves active categories without loading menu item details.
   *
   * @return List of active categories
   */
  List<Category> findByActiveTrue();

  /**
   * Searches for categories by name with pagination.
   * 
   * @param name     The search keyword
   * @param pageable Pagination and sorting information
   * @return Paged result of matching categories
   */
  Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
