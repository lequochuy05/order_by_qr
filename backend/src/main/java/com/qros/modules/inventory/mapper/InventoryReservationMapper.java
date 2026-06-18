package com.qros.modules.inventory.mapper;

import com.qros.modules.inventory.dto.response.InventoryReservationResponse;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.OrderItemInventoryReservation;
import com.qros.modules.inventory.model.enums.InventoryReservationStatus;
import com.qros.modules.order.model.OrderItem;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InventoryReservationMapper {

    public OrderItemInventoryReservation toEntity(
            OrderItem orderItem, InventoryItem inventoryItem, BigDecimal reservedQuantity) {
        return OrderItemInventoryReservation.builder()
                .orderItem(orderItem)
                .inventoryItem(inventoryItem)
                .reservedQuantity(reservedQuantity)
                .status(InventoryReservationStatus.RESERVED)
                .reservedAt(AppTime.now())
                .build();
    }

    public InventoryReservationResponse toResponse(OrderItemInventoryReservation reservation) {
        OrderItem orderItem = reservation.getOrderItem();
        InventoryItem inventoryItem = reservation.getInventoryItem();

        return new InventoryReservationResponse(
                reservation.getId(),
                orderItem != null ? orderItem.getId() : null,
                inventoryItem != null ? inventoryItem.getId() : null,
                inventoryItem != null ? inventoryItem.getName() : null,
                inventoryItem != null ? inventoryItem.getUnit() : null,
                reservation.getReservedQuantity(),
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getReleasedAt(),
                reservation.getConsumedAt());
    }

    public List<InventoryReservationResponse> toResponses(List<OrderItemInventoryReservation> reservations) {
        return reservations.stream().map(this::toResponse).toList();
    }
}
