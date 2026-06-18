package com.qros.modules.inventory.mapper;

import com.qros.modules.inventory.dto.request.InventoryItemRequest;
import com.qros.modules.inventory.dto.response.InventoryItemResponse;
import com.qros.modules.inventory.model.InventoryItem;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InventoryItemMapper {

    public InventoryItem toEntity(InventoryItemRequest request, String normalizedName, String normalizedUnit) {
        return InventoryItem.builder()
                .name(normalizedName)
                .unit(normalizedUnit)
                .quantityOnHand(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .lowStockThreshold(defaultZero(request.lowStockThreshold()))
                .active(request.active() != null ? request.active() : true)
                .build();
    }

    public void updateEntity(
            InventoryItem item, InventoryItemRequest request, String normalizedName, String normalizedUnit) {
        item.setName(normalizedName);
        item.setUnit(normalizedUnit);
        item.setLowStockThreshold(defaultZero(request.lowStockThreshold()));
        item.setActive(request.active() != null ? request.active() : item.getActive());
    }

    public InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getName(),
                item.getUnit(),
                item.getQuantityOnHand(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                item.getLowStockThreshold(),
                item.isLowStock(),
                item.getActive());
    }

    public List<InventoryItemResponse> toResponses(List<InventoryItem> items) {
        return items.stream().map(this::toResponse).toList();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
