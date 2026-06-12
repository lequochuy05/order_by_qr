package com.qros.modules.inventory.controller;

import com.qros.modules.inventory.dto.internal.InventoryReservationResult;
import com.qros.modules.inventory.service.InventoryReservationService;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/inventory/reservations")
@RequiredArgsConstructor
public class InventoryReservationController {

    private final InventoryReservationService inventoryReservationService;

    @PostMapping("/order-items/{orderItemId}/reserve")
    public ApiResponse<InventoryReservationResult> reserveForOrderItem(
            @PathVariable @Min(value = 1, message = "Order item id must be positive") Long orderItemId) {
        return ApiResponse.success(
                "Inventory reserved successfully",
                inventoryReservationService.reserveForOrderItemId(orderItemId));
    }

    @PostMapping("/order-items/{orderItemId}/release")
    public ApiResponse<Void> releaseForOrderItem(
            @PathVariable @Min(value = 1, message = "Order item id must be positive") Long orderItemId) {
        inventoryReservationService.releaseForOrderItemId(orderItemId);

        return ApiResponse.success(
                "Inventory released successfully",
                null);
    }

    @PostMapping("/order-items/{orderItemId}/consume")
    public ApiResponse<Void> consumeForOrderItem(
            @PathVariable @Min(value = 1, message = "Order item id must be positive") Long orderItemId) {
        inventoryReservationService.consumeForOrderItemId(orderItemId);

        return ApiResponse.success(
                "Inventory consumed successfully",
                null);
    }
}