package com.qros.modules.menu.repository;

import com.qros.modules.menu.model.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

  @EntityGraph(attributePaths = {
      "category",
      "itemOptions",
      "itemOptions.optionValues"
  })
  List<MenuItem> findByCategoryIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long categoryId);

  @EntityGraph(attributePaths = { "category" })
  @Query("""
          SELECT m
          FROM MenuItem m
          WHERE m.category.id = :categoryId
            AND m.active = true
          ORDER BY m.displayOrder ASC, m.name ASC
      """)
  List<MenuItem> findActiveSummariesByCategoryId(@Param("categoryId") Long categoryId);

  @Query("""
          SELECT COUNT(m)
          FROM MenuItem m
          WHERE m.category.id = :categoryId
            AND m.active = true
      """)
  long countByCategoryIdAndActiveTrue(@Param("categoryId") Long categoryId);

  boolean existsByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

  @EntityGraph(attributePaths = {
      "category",
      "itemOptions",
      "itemOptions.optionValues"
  })
  Page<MenuItem> findByNameContainingIgnoreCase(String name, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {
      "category",
      "itemOptions",
      "itemOptions.optionValues"
  })
  @NonNull
  Optional<MenuItem> findById(@NonNull Long id);

  @Override
  @EntityGraph(attributePaths = {
      "category",
      "itemOptions",
      "itemOptions.optionValues"
  })
  @NonNull
  List<MenuItem> findAll();

  @EntityGraph(attributePaths = { "category" })
  @Query("""
          SELECT m
          FROM MenuItem m
          ORDER BY m.displayOrder ASC, m.name ASC
      """)
  List<MenuItem> findAllForManagementSummary();

  @EntityGraph(attributePaths = {
      "category",
      "itemOptions",
      "itemOptions.optionValues"
  })
  List<MenuItem> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

  @EntityGraph(attributePaths = {
      "category",
      "itemOptions",
      "itemOptions.optionValues"
  })
  @Query("""
          SELECT DISTINCT m
          FROM MenuItem m
          WHERE m.active = true
            AND m.available = true
            AND m.category.active = true
          ORDER BY m.displayOrder ASC, m.name ASC
      """)
  List<MenuItem> findAllPublicAvailableItems();

  @EntityGraph(attributePaths = {
      "category",
      "itemOptions",
      "itemOptions.optionValues"
  })
  @Query("""
          SELECT DISTINCT m
          FROM MenuItem m
          WHERE m.category.id = :categoryId
            AND m.active = true
            AND m.available = true
            AND m.category.active = true
          ORDER BY m.displayOrder ASC, m.name ASC
      """)
  List<MenuItem> findPublicAvailableItemsByCategoryId(@Param("categoryId") Long categoryId);

  @EntityGraph(attributePaths = { "category" })
  List<MenuItem> findByActiveTrueAndAvailableTrueOrderByIdDesc(Pageable pageable);

  @EntityGraph(attributePaths = { "category" })
  @Query("""
      SELECT m
      FROM MenuItem m
      WHERE m.active = true
        AND m.available = true
        AND m.category.active = true
        AND m.category.id = :categoryId
        AND m.id <> :excludedItemId
      ORDER BY m.displayOrder ASC, m.name ASC
      """)
  List<MenuItem> findSimilarAvailableItems(
      @Param("categoryId") Long categoryId,
      @Param("excludedItemId") Long excludedItemId,
      Pageable pageable);
}