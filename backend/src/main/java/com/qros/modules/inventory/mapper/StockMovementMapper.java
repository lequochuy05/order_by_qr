package com.qros.modules.inventory.mapper;

import com.qros.modules.inventory.dto.response.StockMovementResponse;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.StockMovement;
import com.qros.modules.inventory.model.enums.StockMovementType;
import com.qros.modules.order.model.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StockMovementMapper {

    public StockMovement toEntity(
            InventoryItem inventoryItem,
            OrderItem orderItem,
            StockMovementType type,
            BigDecimal quantity,
            BigDecimal quantityBefore,
            BigDecimal quantityAfter,
            String note) {
        return StockMovement.builder()
                .inventoryItem(inventoryItem)
                .orderItem(orderItem)
                .type(type)
                .quantity(quantity)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .note(normalizeNote(note))
                .build();
    }

    public StockMovementResponse toResponse(StockMovement movement) {
        InventoryItem inventoryItem = movement.getInventoryItem();
        OrderItem orderItem = movement.getOrderItem();

        return new StockMovementResponse(
                movement.getId(),
                inventoryItem != null ? inventoryItem.getId() : null,
                inventoryItem != null ? inventoryItem.getName() : null,
                inventoryItem != null ? inventoryItem.getUnit() : null,
                orderItem != null ? orderItem.getId() : null,
                movement.getType(),
                movement.getQuantity(),
                movement.getQuantityBefore(),
                movement.getQuantityAfter(),
                movement.getNote(),
                movement.getCreatedAt());
    }

    public List<StockMovementResponse> toResponses(List<StockMovement> movements) {
        return movements.stream().map(this::toResponse).toList();
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }

        return note.trim();
    }
}
