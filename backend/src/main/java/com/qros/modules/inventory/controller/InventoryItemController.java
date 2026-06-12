package com.qros.modules.inventory.controller;

import com.qros.modules.inventory.dto.request.InventoryItemRequest;
import com.qros.modules.inventory.dto.request.StockAdjustmentRequest;
import com.qros.modules.inventory.dto.request.StockInRequest;
import com.qros.modules.inventory.dto.response.InventoryItemResponse;
import com.qros.modules.inventory.service.InventoryItemService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/inventory/items")
@RequiredArgsConstructor
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    @GetMapping
    public ApiResponse<Page<InventoryItemResponse>> list(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ApiResponse.success(
                inventoryItemService.search(keyword, pageable));
    }

    @GetMapping("/active")
    public ApiResponse<List<InventoryItemResponse>> activeItems() {
        return ApiResponse.success(
                inventoryItemService.findActiveItems());
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<InventoryItemResponse>> lowStockItems() {
        return ApiResponse.success(
                inventoryItemService.findLowStockItems());
    }

    @GetMapping("/{id}")
    public ApiResponse<InventoryItemResponse> get(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id) {
        return ApiResponse.success(
                inventoryItemService.findById(id));
    }

    @PostMapping
    public ApiResponse<InventoryItemResponse> create(
            @Valid @RequestBody InventoryItemRequest request) {
        return ApiResponse.success(
                "Inventory item created successfully",
                inventoryItemService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<InventoryItemResponse> update(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id,

            @Valid @RequestBody InventoryItemRequest request) {
        return ApiResponse.success(
                "Inventory item updated successfully",
                inventoryItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id) {
        inventoryItemService.delete(id);

        return ApiResponse.success(
                "Inventory item deleted successfully",
                null);
    }

    @PostMapping("/{id}/stock-in")
    public ApiResponse<InventoryItemResponse> stockIn(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id,

            @Valid @RequestBody StockInRequest request) {
        return ApiResponse.success(
                "Stock imported successfully",
                inventoryItemService.stockIn(id, request));
    }

    @PostMapping("/{id}/adjust")
    public ApiResponse<InventoryItemResponse> adjustStock(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id,

            @Valid @RequestBody StockAdjustmentRequest request) {
        return ApiResponse.success(
                "Stock adjusted successfully",
                inventoryItemService.adjustStock(id, request));
    }
}