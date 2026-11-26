package com.qros.modules.inventory.controller;

import com.qros.modules.inventory.dto.request.BatchStockInRequest;
import com.qros.modules.inventory.dto.request.InventoryItemRequest;
import com.qros.modules.inventory.dto.request.StockAdjustmentRequest;
import com.qros.modules.inventory.dto.request.StockInRequest;
import com.qros.modules.inventory.dto.response.BatchStockInResponse;
import com.qros.modules.inventory.dto.response.InventoryItemResponse;
import com.qros.modules.inventory.dto.response.InventorySummaryResponse;
import com.qros.modules.inventory.service.InventoryItemService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

@Validated
@RestController
@RequestMapping(ApiRoutes.INVENTORY_ITEMS)
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    public InventoryItemController(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<Page<InventoryItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stockFilter,
            Pageable pageable) {
        return ApiResponse.success(inventoryItemService.search(keyword, stockFilter, pageable));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<InventorySummaryResponse> summary() {
        return ApiResponse.success(inventoryItemService.getSummary());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<List<InventoryItemResponse>> activeItems() {
        return ApiResponse.success(inventoryItemService.findActiveItems());
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<List<InventoryItemResponse>> lowStockItems() {
        return ApiResponse.success(inventoryItemService.findLowStockItems());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<InventoryItemResponse> get(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id) {
        return ApiResponse.success(inventoryItemService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<InventoryItemResponse> create(@Valid @RequestBody InventoryItemRequest request) {
        return ApiResponse.success("Inventory item created successfully", inventoryItemService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<InventoryItemResponse> update(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id,
            @Valid @RequestBody InventoryItemRequest request) {
        return ApiResponse.success("Inventory item updated successfully", inventoryItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id) {
        inventoryItemService.delete(id);

        return ApiResponse.success("Inventory item deleted successfully", null);
    }

    @PostMapping("/{id}/stock-in")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<InventoryItemResponse> stockIn(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id,
            @Valid @RequestBody StockInRequest request) {
        return ApiResponse.success("Stock imported successfully", inventoryItemService.stockIn(id, request));
    }

    @PostMapping("/batch-stock-in")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<BatchStockInResponse> batchStockIn(
            @Valid @RequestBody BatchStockInRequest request, Authentication authentication) {
        boolean canUpdateUnitCost = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority())
                                || "ROLE_ADMIN".equals(authority.getAuthority()));
        return ApiResponse.success(
                "Batch stock imported successfully", inventoryItemService.batchStockIn(request, canUpdateUnitCost));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ApiResponse<InventoryItemResponse> adjustStock(
            @PathVariable @Min(value = 1, message = "Inventory item id must be positive") Long id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ApiResponse.success("Stock adjusted successfully", inventoryItemService.adjustStock(id, request));
    }
}
