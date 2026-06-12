package com.qros.modules.inventory.repository;

import com.qros.modules.inventory.model.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<InventoryItem> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    List<InventoryItem> findByActiveTrueOrderByNameAsc();

    @Override
    @NonNull
    List<InventoryItem> findAll();

    @Override
    @NonNull
    Optional<InventoryItem> findById(@NonNull Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM InventoryItem i
            WHERE i.id = :id
            """)
    Optional<InventoryItem> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT i
            FROM InventoryItem i
            WHERE i.active = true
              AND (i.quantityOnHand - i.reservedQuantity) > 0
            ORDER BY i.name ASC
            """)
    List<InventoryItem> findAvailableActiveItems();

    @Query("""
            SELECT i
            FROM InventoryItem i
            WHERE i.active = true
              AND (i.quantityOnHand - i.reservedQuantity) <= i.lowStockThreshold
            ORDER BY i.name ASC
            """)
    List<InventoryItem> findLowStockActiveItems();
}