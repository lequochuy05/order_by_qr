package com.qros.modules.inventory.repository;

import com.qros.modules.inventory.model.StockMovement;
import com.qros.modules.inventory.model.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Override
    @NonNull @EntityGraph(attributePaths = {"inventoryItem", "orderItem", "orderItem.menuItem", "orderItem.combo"})
    Page<StockMovement> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"inventoryItem", "orderItem", "orderItem.menuItem", "orderItem.combo"})
    Page<StockMovement> findByInventoryItemIdOrderByIdDesc(Long inventoryItemId, Pageable pageable);

    @EntityGraph(attributePaths = {"inventoryItem", "orderItem", "orderItem.menuItem", "orderItem.combo"})
    Page<StockMovement> findByOrderItemIdOrderByIdDesc(Long orderItemId, Pageable pageable);

    @EntityGraph(attributePaths = {"inventoryItem", "orderItem", "orderItem.menuItem", "orderItem.combo"})
    Page<StockMovement> findByTypeOrderByIdDesc(StockMovementType type, Pageable pageable);
}
