package com.qros.modules.inventory.repository;

import com.qros.modules.inventory.model.OrderItemInventoryReservation;
import com.qros.modules.inventory.model.enums.InventoryReservationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemInventoryReservationRepository extends JpaRepository<OrderItemInventoryReservation, Long> {

    boolean existsByOrderItem_IdAndStatus(Long orderItemId, InventoryReservationStatus status);

    @EntityGraph(attributePaths = {"orderItem", "inventoryItem"})
    List<OrderItemInventoryReservation> findByOrderItem_IdOrderByIdAsc(Long orderItemId);

    @EntityGraph(attributePaths = {"orderItem", "inventoryItem"})
    List<OrderItemInventoryReservation> findByOrderItem_IdAndStatusOrderByIdAsc(
            Long orderItemId, InventoryReservationStatus status);

    @EntityGraph(attributePaths = {"orderItem", "inventoryItem"})
    Optional<OrderItemInventoryReservation> findByOrderItem_IdAndInventoryItem_IdAndStatus(
            Long orderItemId, Long inventoryItemId, InventoryReservationStatus status);

    @EntityGraph(attributePaths = {"orderItem", "inventoryItem"})
    List<OrderItemInventoryReservation> findByInventoryItem_IdAndStatusOrderByIdDesc(
            Long inventoryItemId, InventoryReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT r
            FROM OrderItemInventoryReservation r
            JOIN FETCH r.inventoryItem
            JOIN FETCH r.orderItem
            WHERE r.orderItem.id = :orderItemId
              AND r.status = :status
            ORDER BY r.id ASC
            """)
    List<OrderItemInventoryReservation> findByOrderItemIdAndStatusForUpdate(
            @Param("orderItemId") Long orderItemId, @Param("status") InventoryReservationStatus status);
}
