package com.qros.modules.inventory.mapper;

import com.qros.modules.inventory.dto.request.InventoryItemRequest;
import com.qros.modules.inventory.dto.response.InventoryItemResponse;
import com.qros.modules.inventory.model.InventoryItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
                .unitCost(defaultUnitCost(request.unitCost()))
                .active(request.active() != null ? request.active() : true)
                .build();
    }

    public void updateEntity(
            InventoryItem item, InventoryItemRequest request, String normalizedName, String normalizedUnit) {
        item.setName(normalizedName);
        item.setUnit(normalizedUnit);
        item.setLowStockThreshold(defaultZero(request.lowStockThreshold()));
        if (request.unitCost() != null) {
            item.setUnitCost(defaultUnitCost(request.unitCost()));
        }
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
                defaultUnitCost(item.getUnitCost()),
                item.isLowStock(),
                item.getActive(),
                item.getVersion());
    }

    public List<InventoryItemResponse> toResponses(List<InventoryItem> items) {
        return items.stream().map(this::toResponse).toList();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal defaultUnitCost(BigDecimal value) {
        return value != null
                ? value.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
