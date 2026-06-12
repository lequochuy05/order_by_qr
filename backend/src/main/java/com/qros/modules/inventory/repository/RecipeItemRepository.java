package com.qros.modules.inventory.repository;

import com.qros.modules.inventory.model.RecipeItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, Long> {

    boolean existsByMenuItem_IdAndInventoryItem_Id(Long menuItemId, Long inventoryItemId);

    boolean existsByMenuItem_IdAndInventoryItem_IdAndIdNot(
            Long menuItemId,
            Long inventoryItemId,
            Long id);

    long countByMenuItem_Id(Long menuItemId);

    @EntityGraph(attributePaths = {
            "menuItem",
            "inventoryItem"
    })
    @Query("""
            SELECT r
            FROM RecipeItem r
            WHERE r.menuItem.id = :menuItemId
            ORDER BY r.inventoryItem.name ASC
            """)
    List<RecipeItem> findByMenuItemId(@Param("menuItemId") Long menuItemId);

    @EntityGraph(attributePaths = {
            "menuItem",
            "inventoryItem"
    })
    @Query("""
            SELECT r
            FROM RecipeItem r
            WHERE r.inventoryItem.id = :inventoryItemId
            ORDER BY r.menuItem.name ASC
            """)
    List<RecipeItem> findByInventoryItemId(@Param("inventoryItemId") Long inventoryItemId);

    @EntityGraph(attributePaths = {
            "menuItem",
            "inventoryItem"
    })
    @Query("""
            SELECT r
            FROM RecipeItem r
            WHERE r.menuItem.id IN :menuItemIds
            ORDER BY r.menuItem.id ASC, r.inventoryItem.name ASC
            """)
    List<RecipeItem> findByMenuItemIds(@Param("menuItemIds") List<Long> menuItemIds);
}