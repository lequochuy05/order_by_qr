package com.qros.modules.inventory.service;

import com.qros.modules.inventory.dto.response.StockMovementResponse;
import com.qros.modules.inventory.mapper.StockMovementMapper;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.StockMovement;
import com.qros.modules.inventory.model.enums.StockMovementType;
import com.qros.modules.inventory.repository.StockMovementRepository;
import com.qros.modules.order.model.OrderItem;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final StockMovementMapper stockMovementMapper;

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> findAll(Pageable pageable) {
        return stockMovementRepository.findAll(pageable).map(stockMovementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> findByInventoryItemId(@NonNull Long inventoryItemId, Pageable pageable) {
        return stockMovementRepository
                .findByInventoryItemIdOrderByIdDesc(inventoryItemId, pageable)
                .map(stockMovementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> findByOrderItemId(@NonNull Long orderItemId, Pageable pageable) {
        return stockMovementRepository
                .findByOrderItemIdOrderByIdDesc(orderItemId, pageable)
                .map(stockMovementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> findByType(@NonNull StockMovementType type, Pageable pageable) {
        return stockMovementRepository.findByTypeOrderByIdDesc(type, pageable).map(stockMovementMapper::toResponse);
    }

    @Transactional
    public StockMovement recordMovement(
            @NonNull InventoryItem inventoryItem,
            OrderItem orderItem,
            @NonNull StockMovementType type,
            @NonNull BigDecimal quantity,
            @NonNull BigDecimal quantityBefore,
            @NonNull BigDecimal quantityAfter,
            String note) {
        StockMovement movement = stockMovementMapper.toEntity(
                inventoryItem, orderItem, type, quantity, quantityBefore, quantityAfter, note);

        return stockMovementRepository.save(movement);
    }
}
