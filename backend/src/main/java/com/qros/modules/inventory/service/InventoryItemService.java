package com.qros.modules.inventory.service;

import com.qros.modules.inventory.dto.request.InventoryItemRequest;
import com.qros.modules.inventory.dto.request.StockAdjustmentRequest;
import com.qros.modules.inventory.dto.request.StockInRequest;
import com.qros.modules.inventory.dto.response.InventoryItemResponse;
import com.qros.modules.inventory.dto.response.InventorySummaryResponse;
import com.qros.modules.inventory.mapper.InventoryItemMapper;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.enums.StockMovementType;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private static final int QUANTITY_SCALE = 3;

    private final InventoryItemRepository inventoryItemRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final InventoryItemMapper inventoryItemMapper;
    private final StockMovementService stockMovementService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> findAll(Pageable pageable) {
        return inventoryItemRepository.findAll(pageable)
                .map(inventoryItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> search(String keyword, Pageable pageable) {
        return search(keyword, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> search(String keyword, String stockFilter, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String normalizedStockFilter = stockFilter == null || stockFilter.isBlank() ? "ALL" : stockFilter.trim().toUpperCase();

        return inventoryItemRepository
                .searchForManagement(normalizedKeyword, normalizedStockFilter, pageable)
                .map(inventoryItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummary() {
        return new InventorySummaryResponse(
                inventoryItemRepository.count(),
                inventoryItemRepository.countByActiveTrue(),
                inventoryItemRepository.countLowStockActiveItems(),
                inventoryItemRepository.countOutOfStockActiveItems());
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findActiveItems() {
        return inventoryItemMapper.toResponses(
                inventoryItemRepository.findByActiveTrueOrderByNameAsc());
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findLowStockItems() {
        return inventoryItemMapper.toResponses(
                inventoryItemRepository.findLowStockActiveItems());
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse findById(@NonNull Long id) {
        return inventoryItemMapper.toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public InventoryItem getEntityById(@NonNull Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_ITEM_NOT_FOUND));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.INVENTORY, allEntries = true),
            @CacheEvict(value = CacheNames.PUBLIC_MENU, allEntries = true),
            @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
            @CacheEvict(value = CacheNames.AI_MENU_CONTEXT, allEntries = true)
    })
    public InventoryItemResponse create(@NonNull InventoryItemRequest request) {
        String normalizedName = normalizeName(request.name());
        String normalizedUnit = normalizeUnit(request.unit());

        if (inventoryItemRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BusinessException(ErrorCode.INVENTORY_ITEM_NAME_EXISTS);
        }

        InventoryItem item = inventoryItemMapper.toEntity(
                request,
                normalizedName,
                normalizedUnit);

        InventoryItem saved = inventoryItemRepository.save(item);

        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_item_created", saved.getId()));

        return inventoryItemMapper.toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.INVENTORY, allEntries = true),
            @CacheEvict(value = CacheNames.PUBLIC_MENU, allEntries = true),
            @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
            @CacheEvict(value = CacheNames.AI_MENU_CONTEXT, allEntries = true)
    })
    public InventoryItemResponse update(
            @NonNull Long id,
            @NonNull InventoryItemRequest request) {
        InventoryItem item = getEntityById(id);

        String normalizedName = normalizeName(request.name());
        String normalizedUnit = normalizeUnit(request.unit());

        if (inventoryItemRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new BusinessException(ErrorCode.INVENTORY_ITEM_NAME_EXISTS);
        }

        inventoryItemMapper.updateEntity(
                item,
                request,
                normalizedName,
                normalizedUnit);

        InventoryItem saved = inventoryItemRepository.save(item);

        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_item_updated", saved.getId()));

        return inventoryItemMapper.toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.INVENTORY, allEntries = true),
            @CacheEvict(value = CacheNames.PUBLIC_MENU, allEntries = true),
            @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
            @CacheEvict(value = CacheNames.AI_MENU_CONTEXT, allEntries = true)
    })
    public void delete(@NonNull Long id) {
        InventoryItem item = getEntityById(id);

        if (!recipeItemRepository.findByInventoryItemId(id).isEmpty()) {
            throw new BusinessException(ErrorCode.INVENTORY_ITEM_IN_USE);
        }

        inventoryItemRepository.delete(item);

        eventPublisher.publishEvent(new InventoryChangeEvent("inventory_item_deleted", id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.INVENTORY, allEntries = true),
            @CacheEvict(value = CacheNames.PUBLIC_MENU, allEntries = true),
            @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
            @CacheEvict(value = CacheNames.AI_MENU_CONTEXT, allEntries = true)
    })
    public InventoryItemResponse stockIn(
            @NonNull Long itemId,
            @NonNull StockInRequest request) {
        InventoryItem item = getEntityByIdForUpdate(itemId);

        BigDecimal quantity = normalizePositiveQuantity(request.quantity());
        BigDecimal quantityBefore = normalizeQuantity(item.getQuantityOnHand());
        BigDecimal quantityAfter = quantityBefore.add(quantity);

        item.setQuantityOnHand(quantityAfter);

        InventoryItem saved = inventoryItemRepository.save(item);

        stockMovementService.recordMovement(
                saved,
                null,
                StockMovementType.STOCK_IN,
                quantity,
                quantityBefore,
                quantityAfter,
                request.note());

        eventPublisher.publishEvent(new InventoryChangeEvent("stock_in", saved.getId()));

        return inventoryItemMapper.toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.INVENTORY, allEntries = true),
            @CacheEvict(value = CacheNames.PUBLIC_MENU, allEntries = true),
            @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
            @CacheEvict(value = CacheNames.AI_MENU_CONTEXT, allEntries = true)
    })
    public InventoryItemResponse adjustStock(
            @NonNull Long itemId,
            @NonNull StockAdjustmentRequest request) {
        InventoryItem item = getEntityByIdForUpdate(itemId);

        BigDecimal quantityDelta = normalizeQuantity(request.quantityDelta());

        if (quantityDelta.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        BigDecimal quantityBefore = normalizeQuantity(item.getQuantityOnHand());
        BigDecimal quantityAfter = quantityBefore.add(quantityDelta);

        if (quantityAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }

        if (quantityAfter.compareTo(normalizeQuantity(item.getReservedQuantity())) < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QUANTITY_BELOW_RESERVED);
        }

        item.setQuantityOnHand(quantityAfter);

        InventoryItem saved = inventoryItemRepository.save(item);

        stockMovementService.recordMovement(
                saved,
                null,
                StockMovementType.ADJUSTMENT,
                quantityDelta,
                quantityBefore,
                quantityAfter,
                request.note());

        eventPublisher.publishEvent(new InventoryChangeEvent("stock_adjusted", saved.getId()));

        return inventoryItemMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryItem getEntityByIdForUpdate(@NonNull Long id) {
        return inventoryItemRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_ITEM_NOT_FOUND));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return name.trim();
    }

    private String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return unit.trim().toLowerCase();
    }

    private BigDecimal normalizePositiveQuantity(BigDecimal value) {
        BigDecimal quantity = normalizeQuantity(value);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }

        return quantity;
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }

        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }
}
