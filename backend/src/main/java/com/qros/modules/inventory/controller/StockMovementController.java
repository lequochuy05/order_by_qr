package com.qros.modules.inventory.controller;

import com.qros.modules.inventory.dto.response.StockMovementResponse;
import com.qros.modules.inventory.model.enums.StockMovementType;
import com.qros.modules.inventory.service.StockMovementService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiRoutes.INVENTORY_MOVEMENTS)
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @GetMapping
    public ApiResponse<Page<StockMovementResponse>> list(
            @RequestParam(required = false) @Min(value = 1, message = "Inventory item id must be positive")
                    Long inventoryItemId,
            @RequestParam(required = false) @Min(value = 1, message = "Order item id must be positive")
                    Long orderItemId,
            @RequestParam(required = false) StockMovementType type,
            Pageable pageable) {
        if (inventoryItemId != null) {
            return ApiResponse.success(stockMovementService.findByInventoryItemId(inventoryItemId, pageable));
        }

        if (orderItemId != null) {
            return ApiResponse.success(stockMovementService.findByOrderItemId(orderItemId, pageable));
        }

        if (type != null) {
            return ApiResponse.success(stockMovementService.findByType(type, pageable));
        }

        return ApiResponse.success(stockMovementService.findAll(pageable));
    }
}
