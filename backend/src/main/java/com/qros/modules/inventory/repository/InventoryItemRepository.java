package com.qros.modules.inventory.repository;

import com.qros.modules.inventory.model.InventoryItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<InventoryItem> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    @Query(
            """
            SELECT i
            FROM InventoryItem i
            WHERE (CAST(:keyword AS string) IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(i.unit) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (
                    CAST(:stockFilter AS string) IS NULL
                    OR :stockFilter = 'ALL'
                    OR (:stockFilter = 'LOW' AND i.active = true AND (i.quantityOnHand - i.reservedQuantity) <= i.lowStockThreshold)
                    OR (:stockFilter = 'OUT' AND i.active = true AND (i.quantityOnHand - i.reservedQuantity) <= 0)
                    OR (:stockFilter = 'INACTIVE' AND i.active = false)
                  )
            ORDER BY i.name ASC
            """)
    Page<InventoryItem> searchForManagement(
            @Param("keyword") String keyword, @Param("stockFilter") String stockFilter, Pageable pageable);

    List<InventoryItem> findByActiveTrueOrderByNameAsc();

    long countByActiveTrue();

    @Query(
            """
            SELECT COUNT(i)
            FROM InventoryItem i
            WHERE i.active = true
              AND (i.quantityOnHand - i.reservedQuantity) <= i.lowStockThreshold
            """)
    long countLowStockActiveItems();

    @Query(
            """
            SELECT COUNT(i)
            FROM InventoryItem i
            WHERE i.active = true
              AND (i.quantityOnHand - i.reservedQuantity) <= 0
            """)
    long countOutOfStockActiveItems();

    @Override
    @NonNull List<InventoryItem> findAll();

    @Override
    @NonNull Optional<InventoryItem> findById(@NonNull Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM InventoryItem i
            WHERE i.id = :id
            """)
    Optional<InventoryItem> findByIdForUpdate(@Param("id") Long id);

    @Query(
            """
            SELECT i
            FROM InventoryItem i
            WHERE i.active = true
              AND (i.quantityOnHand - i.reservedQuantity) > 0
            ORDER BY i.name ASC
            """)
    List<InventoryItem> findAvailableActiveItems();

    @Query(
            """
            SELECT i
            FROM InventoryItem i
            WHERE i.active = true
              AND (i.quantityOnHand - i.reservedQuantity) <= i.lowStockThreshold
            ORDER BY i.name ASC
            """)
    List<InventoryItem> findLowStockActiveItems();
}
